"""Farm data endpoints - water quality data sync and retrieval."""

from typing import Annotated, Optional
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session
import uuid

from app.core.database import get_db
from app.api.deps import get_current_user
from app.models.user import User
from app.schemas.farm_data import (
    FarmDataSyncRequest,
    FarmDataSyncResponse,
    FarmDataListResponse,
    FarmDataResponse,
    AnalyticsResponse,
    SyncedReading,
)
from app.schemas.common import SuccessResponse
from app.services.farm_data_service import FarmDataService

router = APIRouter()


@router.post(
    "/sync",
    response_model=SuccessResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Sync farm data",
    description="Bulk sync water quality readings from mobile device. Only water parameters and location are stored.",
)
def sync_farm_data(
    sync_data: FarmDataSyncRequest,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> SuccessResponse:
    """Sync farm data readings."""
    try:
        created_readings, failed_count = FarmDataService.bulk_create_readings(
            db=db,
            user_id=str(current_user.id),
            readings=sync_data.readings,
            device_id=sync_data.device_id,
        )

        sync_id = str(uuid.uuid4())
        synced_readings = [
            SyncedReading(
                data_id=str(reading.id),
                recorded_at=reading.recorded_at,
                status="success",
            )
            for reading in created_readings
        ]

        response_data = FarmDataSyncResponse(
            synced_count=len(created_readings),
            failed_count=failed_count,
            sync_id=sync_id,
            synced_at=datetime.utcnow(),
            readings=synced_readings,
        )

        return SuccessResponse(success=True, data=response_data.model_dump())

    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to sync farm data: {str(e)}",
        )


@router.get(
    "",
    response_model=SuccessResponse,
    summary="Get farm data",
    description="Get user's historical water quality readings.",
)
def get_farm_data(
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    start_date: Optional[datetime] = Query(None, description="Filter by start date"),
    end_date: Optional[datetime] = Query(None, description="Filter by end date"),
    limit: int = Query(100, ge=1, le=1000, description="Max records to return"),
    offset: int = Query(0, ge=0, description="Pagination offset"),
) -> SuccessResponse:
    """Get user's farm data readings."""
    readings, total = FarmDataService.get_user_readings(
        db=db,
        user_id=str(current_user.id),
        start_date=start_date,
        end_date=end_date,
        limit=limit,
        offset=offset,
    )

    reading_responses = [
        FarmDataResponse(
            data_id=str(r.id),
            temperature=float(r.temperature),
            ph=float(r.ph),
            dissolved_oxygen=float(r.dissolved_oxygen),
            ammonia=float(r.ammonia),
            nitrate=float(r.nitrate),
            turbidity=float(r.turbidity),
            fish_weight=float(r.fish_weight) if r.fish_weight else None,
            fish_length=float(r.fish_length) if r.fish_length else None,
            verified=r.verified,
            start_date=r.start_date.isoformat() if r.start_date else None,
            country_code=r.country_code,
            recorded_at=r.recorded_at,
            synced_at=r.synced_at,
        )
        for r in readings
    ]

    response_data = FarmDataListResponse(
        readings=reading_responses,
        total=total,
        limit=limit,
        offset=offset,
    )

    return SuccessResponse(success=True, data=response_data.model_dump())


@router.get(
    "/analytics",
    response_model=SuccessResponse,
    summary="Get analytics",
    description="Get aggregated water quality analytics grouped by location.",
)
def get_analytics(
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    region: Optional[str] = Query(None, description="Filter by country code"),
    start_date: Optional[datetime] = Query(None, description="Filter by start date"),
    end_date: Optional[datetime] = Query(None, description="Filter by end date"),
) -> SuccessResponse:
    """Get aggregated analytics."""
    analytics = FarmDataService.get_regional_analytics(
        db=db,
        country_code=region,
        start_date=start_date,
        end_date=end_date,
    )

    response_data = AnalyticsResponse(aggregated_by_region=analytics)

    return SuccessResponse(success=True, data=response_data.model_dump())


@router.delete(
    "/user",
    response_model=SuccessResponse,
    summary="Delete user data",
    description="Delete all water quality data for authenticated user (GDPR compliance).",
)
def delete_user_data(
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> SuccessResponse:
    """Delete all user's farm data."""
    deleted_count = FarmDataService.delete_user_data(db, str(current_user.id))

    return SuccessResponse(
        success=True,
        data={
            "deleted_count": deleted_count,
            "message": "All your water quality data has been permanently deleted",
        },
    )
