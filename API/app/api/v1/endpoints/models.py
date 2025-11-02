"""ML model distribution endpoints."""

from typing import Annotated, Optional
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.schemas.model import ModelVersionResponse, ModelUpdateCheckResponse
from app.schemas.common import SuccessResponse
from app.services.model_service import ModelService

router = APIRouter()


@router.get(
    "/latest",
    response_model=SuccessResponse,
    summary="Get latest model",
    description="Get information about the latest active ML model version.",
)
def get_latest_model(
    db: Annotated[Session, Depends(get_db)],
) -> SuccessResponse:
    """Get latest model version."""
    model = ModelService.get_latest_model(db)

    if not model:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No active model version found",
        )

    response_data = ModelVersionResponse(
        model_id=str(model.id),
        version=model.version,
        model_url=model.model_url,
        model_size_bytes=model.model_size_bytes,
        preprocessing_config=model.preprocessing_config,
        is_active=model.is_active,
        min_app_version=model.min_app_version,
        created_at=model.created_at,
    )

    return SuccessResponse(success=True, data=response_data.model_dump())


@router.get(
    "/check-update",
    response_model=SuccessResponse,
    summary="Check for model update",
    description="Check if a newer model version is available compared to current version.",
)
def check_model_update(
    db: Annotated[Session, Depends(get_db)],
    current_version: str = Query(..., description="Current model version"),
    app_version: Optional[str] = Query(None, description="App version for compatibility check"),
) -> SuccessResponse:
    """Check if model update is available."""
    update_available, latest_model = ModelService.check_for_update(db, current_version)

    if not latest_model:
        response_data = ModelUpdateCheckResponse(
            update_available=False,
            current_version=current_version,
        )
    elif not update_available:
        response_data = ModelUpdateCheckResponse(
            update_available=False,
            current_version=current_version,
            latest_version=latest_model.version,
        )
    else:
        # Check compatibility if app_version provided
        compatible = ModelService.is_version_compatible(latest_model, app_version)

        if not compatible:
            response_data = ModelUpdateCheckResponse(
                update_available=False,
                current_version=current_version,
                latest_version=latest_model.version,
                release_notes=f"Update requires app version {latest_model.min_app_version} or higher",
            )
        else:
            response_data = ModelUpdateCheckResponse(
                update_available=True,
                current_version=current_version,
                latest_version=latest_model.version,
                model_url=latest_model.model_url,
                model_size_bytes=latest_model.model_size_bytes,
                release_notes="New model version available with improved accuracy",
            )

    return SuccessResponse(success=True, data=response_data.model_dump())
