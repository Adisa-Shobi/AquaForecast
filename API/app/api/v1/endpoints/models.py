"""ML model distribution and management endpoints."""

import logging
import asyncio
import json
import threading
from typing import Annotated, Optional
from fastapi import APIRouter, Depends, HTTPException, status, Query, BackgroundTasks, Request
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.schemas.model import (
    ModelVersionResponse,
    ModelListResponse,
    ModelUpdateCheckResponse,
    RetrainRequest,
    DeployModelRequest,
    ModelMetricsResponse,
    TrainingStatusResponse,
    ModelStatusEnum,
)
from app.schemas.common import SuccessResponse
from app.services.model_service import ModelService
from app.services.model_training_service import ModelTrainingService
from app.api.deps import get_current_user
from app.models.user import User

router = APIRouter()
logger = logging.getLogger(__name__)


def _run_training_task_in_thread(train_params: dict, event_loop):
    """
    Training task that runs in a completely separate daemon thread.

    This function runs outside the FastAPI event loop entirely,
    ensuring the web server remains fully responsive.

    Resource limits are applied to prevent training from monopolizing CPU.
    """
    import os
    import sys
    from app.core.database import SessionLocal
    from app.models.training_task import TrainingTask, TrainingTaskStatus
    from app.core.events import training_events
    from datetime import datetime

    # Limit TensorFlow to use fewer CPU threads to leave resources for API
    # This prevents training from monopolizing all CPU cores
    os.environ['TF_NUM_INTRAOP_THREADS'] = '2'  # Limit parallel ops within a single operation
    os.environ['TF_NUM_INTEROP_THREADS'] = '2'  # Limit parallel independent operations
    os.environ['OMP_NUM_THREADS'] = '2'  # Limit OpenMP threads

    # Lower thread priority on Unix systems
    try:
        import os
        if hasattr(os, 'nice'):
            os.nice(10)  # Lower priority (higher nice value = lower priority)
            logger.info(f"Training thread priority lowered (nice +10)")
    except Exception as e:
        logger.warning(f"Could not lower thread priority: {e}")

    task_id = train_params["task_id"]
    task_db = SessionLocal()

    try:
        # Update task status to RUNNING
        task_record = task_db.query(TrainingTask).filter(TrainingTask.id == task_id).first()
        if task_record:
            task_record.status = TrainingTaskStatus.RUNNING
            task_record.started_at = datetime.utcnow()
            task_record.current_stage = "Initializing training"
            task_db.commit()

        logger.info(f"Training task {task_id} starting in background thread")

        # Notify SSE streams that training has started
        if event_loop:
            try:
                asyncio.run_coroutine_threadsafe(
                    training_events.notify_training_started(str(task_id)),
                    event_loop
                )
            except Exception as e:
                logger.warning(f"Failed to notify training started: {e}")

        # Run the actual training with event loop for cross-thread SSE notifications
        new_model = ModelTrainingService.train_model(
            db=task_db,
            base_model_id=train_params["base_model_id"],
            new_version=train_params["new_version"],
            user_id=train_params["user_id"],
            epochs=train_params["epochs"],
            batch_size=train_params["batch_size"],
            learning_rate=train_params["learning_rate"],
            notes=train_params["notes"],
            task_id=task_id,
            event_loop=event_loop,  # Pass event loop for cross-thread SSE notifications
        )

        # Update task status to COMPLETED
        task_record = task_db.query(TrainingTask).filter(TrainingTask.id == task_id).first()
        if task_record:
            task_record.status = TrainingTaskStatus.COMPLETED
            task_record.completed_at = datetime.utcnow()
            task_record.result_model_id = new_model.id
            task_record.progress_percentage = 100.0
            task_record.current_stage = "Training completed"
            task_db.commit()

        logger.info(f"Model {new_model.version} trained successfully")

        # Notify SSE streams that training has completed
        if event_loop:
            try:
                asyncio.run_coroutine_threadsafe(
                    training_events.notify_training_completed(str(task_id)),
                    event_loop
                )
            except Exception as e:
                logger.warning(f"Failed to notify training completed: {e}")

    except Exception as e:
        logger.error(f"Training failed: {str(e)}", exc_info=True)
        task_record = task_db.query(TrainingTask).filter(TrainingTask.id == task_id).first()
        if task_record:
            task_record.status = TrainingTaskStatus.FAILED
            task_record.completed_at = datetime.utcnow()
            task_record.error_message = str(e)
            task_db.commit()

        # Notify SSE streams that training has failed
        if event_loop:
            try:
                asyncio.run_coroutine_threadsafe(
                    training_events.notify_training_completed(str(task_id)),
                    event_loop
                )
            except Exception as e:
                logger.warning(f"Failed to notify training completion: {e}")
    finally:
        task_db.close()


@router.get(
    "/latest",
    response_model=SuccessResponse,
    summary="Get latest model",
    description="Get the currently deployed model if one exists, otherwise returns the latest active model version. This is the recommended endpoint for mobile apps to download the production model.",
)
def get_latest_model(
    db: Annotated[Session, Depends(get_db)],
) -> SuccessResponse:
    """
    Get latest model version for download.

    Returns the deployed model if one exists, otherwise the latest active model.
    This ensures mobile apps always get the production-ready model.
    """
    model = ModelService.get_latest_model(db)

    if not model:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No active model version found",
        )

    response_data = ModelVersionResponse(
        model_id=str(model.id),
        version=model.version,
        tflite_model_url=model.tflite_model_url,
        keras_model_url=model.keras_model_url,
        tflite_size_bytes=model.tflite_size_bytes,
        keras_size_bytes=model.keras_size_bytes,
        base_model_id=str(model.base_model_id) if model.base_model_id else None,
        base_model_version=model.base_model.version if model.base_model else None,
        preprocessing_config=model.preprocessing_config,
        model_config=model.model_config,
        training_data_count=model.training_data_count,
        training_duration_seconds=model.training_duration_seconds,
        metrics=model.metrics,
        status=ModelStatusEnum(model.status.value),
        is_deployed=model.is_deployed,
        is_active=model.is_active,
        min_app_version=model.min_app_version,
        created_at=model.created_at,
        deployed_at=model.deployed_at,
        notes=model.notes,
    )

    return SuccessResponse(success=True, data=response_data.model_dump())


@router.get(
    "/deployed",
    response_model=SuccessResponse,
    summary="Get deployed model",
    description="Get information about the currently deployed production model.",
)
def get_deployed_model(
    db: Annotated[Session, Depends(get_db)],
) -> SuccessResponse:
    """Get currently deployed model."""
    model = ModelService.get_deployed_model(db)

    if not model:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No deployed model found",
        )

    response_data = ModelVersionResponse(
        model_id=str(model.id),
        version=model.version,
        tflite_model_url=model.tflite_model_url,
        keras_model_url=model.keras_model_url,
        tflite_size_bytes=model.tflite_size_bytes,
        keras_size_bytes=model.keras_size_bytes,
        base_model_id=str(model.base_model_id) if model.base_model_id else None,
        base_model_version=model.base_model.version if model.base_model else None,
        preprocessing_config=model.preprocessing_config,
        model_config=model.model_config,
        training_data_count=model.training_data_count,
        training_duration_seconds=model.training_duration_seconds,
        metrics=model.metrics,
        status=ModelStatusEnum(model.status.value),
        is_deployed=model.is_deployed,
        is_active=model.is_active,
        min_app_version=model.min_app_version,
        created_at=model.created_at,
        deployed_at=model.deployed_at,
        notes=model.notes,
    )

    return SuccessResponse(success=True, data=response_data.model_dump())


@router.get(
    "/list",
    response_model=SuccessResponse,
    summary="List all models",
    description="Get a list of all trained models with their metadata.",
)
def list_all_models(
    db: Annotated[Session, Depends(get_db)],
    include_archived: bool = Query(False, description="Include archived models"),
) -> SuccessResponse:
    """List all models."""
    models = ModelService.get_all_models(db, include_archived=include_archived)
    deployed_model = ModelService.get_deployed_model(db)

    model_responses = [
        ModelVersionResponse(
            model_id=str(m.id),
            version=m.version,
            tflite_model_url=m.tflite_model_url,
            keras_model_url=m.keras_model_url,
            tflite_size_bytes=m.tflite_size_bytes,
            keras_size_bytes=m.keras_size_bytes,
            base_model_id=str(m.base_model_id) if m.base_model_id else None,
            base_model_version=m.base_model.version if m.base_model else None,
            preprocessing_config=m.preprocessing_config,
            model_config=m.model_config,
            training_data_count=m.training_data_count,
            training_duration_seconds=m.training_duration_seconds,
            metrics=m.metrics,
            status=ModelStatusEnum(m.status.value),
            is_deployed=m.is_deployed,
            is_active=m.is_active,
            min_app_version=m.min_app_version,
            created_at=m.created_at,
            deployed_at=m.deployed_at,
            notes=m.notes,
        )
        for m in models
    ]

    response_data = ModelListResponse(
        models=model_responses,
        total_count=len(model_responses),
        deployed_model_id=str(deployed_model.id) if deployed_model else None,
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
                model_url=latest_model.tflite_model_url,
                model_size_bytes=latest_model.tflite_size_bytes,
                release_notes=latest_model.notes or "New model version available with improved accuracy",
            )

    return SuccessResponse(success=True, data=response_data.model_dump())


@router.get(
    "/{model_id}/metrics",
    response_model=SuccessResponse,
    summary="Get model metrics",
    description="Get detailed metrics and training information for a specific model.",
)
def get_model_metrics(
    model_id: str,
    db: Annotated[Session, Depends(get_db)],
) -> SuccessResponse:
    """Get detailed model metrics."""
    try:
        metrics_data = ModelService.get_model_metrics(db, model_id)

        response_data = ModelMetricsResponse(
            model_id=metrics_data['model_id'],
            version=metrics_data['version'],
            metrics=metrics_data['metrics'],
            training_data_count=metrics_data['training_data_count'],
            training_duration_seconds=metrics_data['training_duration_seconds'],
            training_history=metrics_data['training_history'],
            created_at=metrics_data['created_at'],
        )

        return SuccessResponse(success=True, data=response_data.model_dump())

    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e),
        )


@router.post(
    "/retrain",
    response_model=SuccessResponse,
    summary="Retrain model",
    description="Train a new model based on an existing base model using unused farm data.",
    status_code=status.HTTP_202_ACCEPTED,
)
async def retrain_model(
    request: RetrainRequest,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
) -> SuccessResponse:
    """
    Retrain model from a base model.

    This endpoint initiates model retraining as a background task that runs
    in a separate thread pool, preventing the API server from blocking.

    Architecture:
    1. Creates TrainingTask record in database (status: PENDING)
    2. Returns task_id immediately to client (API remains responsive)
    3. Schedules background task using FastAPI BackgroundTasks
    4. Background task uses asyncio.to_thread() to run training in thread pool
    5. Training thread updates progress via asyncio.run_coroutine_threadsafe()
    6. SSE streams receive real-time updates via event bus

    The API server remains fully responsive during training, which can take
    several minutes depending on dataset size and number of epochs.
    """
    from app.models.training_task import TrainingTask, TrainingTaskStatus
    from datetime import datetime
    import uuid

    try:
        # Validate inputs
        if not request.base_model_id:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="base_model_id is required",
            )

        # Create training task
        task = TrainingTask(
            id=uuid.uuid4(),
            base_model_id=request.base_model_id,
            new_version=request.new_version,
            initiated_by=current_user.id,
            status=TrainingTaskStatus.PENDING,
            training_params={
                "epochs": request.epochs,
                "batch_size": request.batch_size,
                "learning_rate": request.learning_rate,
                "notes": request.notes,
            },
            created_at=datetime.utcnow(),
        )
        db.add(task)
        db.commit()
        db.refresh(task)

        task_id = task.id

        # Capture request parameters for background task
        train_params = {
            "base_model_id": request.base_model_id,
            "new_version": request.new_version,
            "user_id": str(current_user.id),
            "epochs": request.epochs,
            "batch_size": request.batch_size,
            "learning_rate": request.learning_rate,
            "notes": request.notes,
            "task_id": str(task_id),
        }

        # Get event loop for cross-thread notifications
        try:
            event_loop = asyncio.get_running_loop()
        except RuntimeError:
            event_loop = None
            logger.warning("No event loop available for training notifications")

        # Start training in a completely separate daemon thread
        # This ensures the response returns immediately without waiting
        training_thread = threading.Thread(
            target=_run_training_task_in_thread,
            args=(train_params, event_loop),
            daemon=True,  # Daemon thread won't prevent app shutdown
            name=f"training-{task_id}"
        )
        training_thread.start()
        logger.info(f"Training thread started for task {task_id}")

        return SuccessResponse(
            success=True,
            data={
                "message": f"Model retraining initiated for version {request.new_version}",
                "task_id": str(task_id),
                "base_model_id": request.base_model_id,
                "new_version": request.new_version,
                "status": "pending",
            }
        )

    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to initiate retraining: {str(e)}",
        )


@router.post(
    "/deploy",
    response_model=SuccessResponse,
    summary="Deploy model",
    description="Deploy a completed model to production.",
)
def deploy_model(
    request: DeployModelRequest,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
) -> SuccessResponse:
    """Deploy a model to production."""
    try:
        deployed_model = ModelService.deploy_model(
            db=db,
            model_id=request.model_id,
            notes=request.notes,
        )

        response_data = ModelVersionResponse(
            model_id=str(deployed_model.id),
            version=deployed_model.version,
            tflite_model_url=deployed_model.tflite_model_url,
            keras_model_url=deployed_model.keras_model_url,
            tflite_size_bytes=deployed_model.tflite_size_bytes,
            keras_size_bytes=deployed_model.keras_size_bytes,
            base_model_id=str(deployed_model.base_model_id) if deployed_model.base_model_id else None,
            base_model_version=deployed_model.base_model.version if deployed_model.base_model else None,
            preprocessing_config=deployed_model.preprocessing_config,
            model_config=deployed_model.model_config,
            training_data_count=deployed_model.training_data_count,
            training_duration_seconds=deployed_model.training_duration_seconds,
            metrics=deployed_model.metrics,
            status=ModelStatusEnum(deployed_model.status.value),
            is_deployed=deployed_model.is_deployed,
            is_active=deployed_model.is_active,
            min_app_version=deployed_model.min_app_version,
            created_at=deployed_model.created_at,
            deployed_at=deployed_model.deployed_at,
            notes=deployed_model.notes,
        )

        return SuccessResponse(
            success=True,
            data=response_data.model_dump(),
        )

    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        )


@router.post(
    "/{model_id}/undeploy",
    response_model=SuccessResponse,
    summary="Undeploy model",
    description="Undeploy a model from production. The model will remain active but not deployed.",
)
def undeploy_model(
    model_id: str,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
) -> SuccessResponse:
    """Undeploy a model from production."""
    try:
        undeployed_model = ModelService.undeploy_model(
            db=db,
            model_id=model_id,
        )

        response_data = ModelVersionResponse(
            model_id=str(undeployed_model.id),
            version=undeployed_model.version,
            tflite_model_url=undeployed_model.tflite_model_url,
            keras_model_url=undeployed_model.keras_model_url,
            tflite_size_bytes=undeployed_model.tflite_size_bytes,
            keras_size_bytes=undeployed_model.keras_size_bytes,
            base_model_id=str(undeployed_model.base_model_id) if undeployed_model.base_model_id else None,
            base_model_version=undeployed_model.base_model.version if undeployed_model.base_model else None,
            preprocessing_config=undeployed_model.preprocessing_config,
            model_config=undeployed_model.model_config,
            training_data_count=undeployed_model.training_data_count,
            training_duration_seconds=undeployed_model.training_duration_seconds,
            metrics=undeployed_model.metrics,
            status=ModelStatusEnum(undeployed_model.status.value),
            is_deployed=undeployed_model.is_deployed,
            is_active=undeployed_model.is_active,
            min_app_version=undeployed_model.min_app_version,
            created_at=undeployed_model.created_at,
            deployed_at=undeployed_model.deployed_at,
            notes=undeployed_model.notes,
        )

        return SuccessResponse(
            success=True,
            data=response_data.model_dump(),
        )

    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        )


@router.delete(
    "/{model_id}/archive",
    response_model=SuccessResponse,
    summary="Archive model",
    description="Archive a model (cannot be deployed or currently deployed).",
)
def archive_model(
    model_id: str,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
) -> SuccessResponse:
    """Archive a model."""
    try:
        archived_model = ModelService.archive_model(db=db, model_id=model_id)

        return SuccessResponse(
            success=True,
            data={
                "model_id": str(archived_model.id),
                "version": archived_model.version,
                "status": archived_model.status.value,
                "message": f"Model {archived_model.version} archived successfully",
            }
        )

    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        )


@router.get(
    "/training/tasks",
    response_model=SuccessResponse,
    summary="Get training tasks",
    description="Get all training tasks or filter by status.",
)
def get_training_tasks(
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
    status_filter: Optional[str] = Query(None, description="Filter by status: pending, running, completed, failed"),
    limit: int = Query(50, le=100),
) -> SuccessResponse:
    """Get training tasks."""
    from app.models.training_task import TrainingTask, TrainingTaskStatus

    query = db.query(TrainingTask)

    if status_filter:
        try:
            task_status = TrainingTaskStatus(status_filter)
            query = query.filter(TrainingTask.status == task_status)
        except ValueError:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Invalid status: {status_filter}",
            )

    tasks = query.order_by(TrainingTask.created_at.desc()).limit(limit).all()

    return SuccessResponse(
        success=True,
        data={
            "tasks": [task.to_dict() for task in tasks],
            "total": len(tasks),
        }
    )


@router.get(
    "/training/tasks/stream",
    summary="Stream training progress (SSE)",
    description="Server-Sent Events stream for real-time training progress updates.",
)
async def stream_training_progress(
    current_user: Annotated[User, Depends(get_current_user)],
):
    """
    Stream training progress using Server-Sent Events.

    This endpoint provides real-time updates for active training tasks
    without the need for client-side polling.

    Authentication is handled via standard Authorization header (same as other endpoints).

    Connection pool optimization: Creates and closes DB sessions per query to avoid
    holding connections open for the duration of the SSE stream.
    """
    from app.models.training_task import TrainingTask, TrainingTaskStatus
    from app.core.database import SessionLocal

    # User is already authenticated via get_current_user dependency
    logger.info(f"SSE stream started for user: {current_user.email}")

    async def event_generator():
        """Generate SSE events for training progress."""
        from app.core.events import training_events

        last_states = {}  # Track last known state of each task

        # Send initial comment to establish connection immediately
        # This prevents buffering issues on DigitalOcean/CloudFlare
        yield ": ping\n\n"

        try:
            while True:
                # Wait for a training event or timeout after 30 seconds
                event_occurred = await training_events.wait_for_update(timeout=30.0)

                # CRITICAL: Create new session, query, then immediately close
                # This prevents holding DB connections for the entire SSE duration
                task_db = SessionLocal()
                try:
                    # Get active tasks (pending or running)
                    active_tasks = task_db.query(TrainingTask).filter(
                        TrainingTask.status.in_([TrainingTaskStatus.PENDING, TrainingTaskStatus.RUNNING])
                    ).all()

                    # Also get recently completed/failed tasks (last 5 minutes)
                    from datetime import datetime, timedelta
                    recent_cutoff = datetime.utcnow() - timedelta(minutes=5)
                    recent_completed = task_db.query(TrainingTask).filter(
                        TrainingTask.status.in_([TrainingTaskStatus.COMPLETED, TrainingTaskStatus.FAILED]),
                        TrainingTask.completed_at >= recent_cutoff
                    ).all()

                    all_tasks = active_tasks + recent_completed

                    # Convert to dictionaries while session is open
                    task_dicts = [(str(task.id), task.to_dict(), (
                        task.status.value,
                        task.progress_percentage,
                        task.current_epoch,
                        task.current_stage
                    )) for task in all_tasks]

                finally:
                    # CRITICAL: Close session immediately after query
                    # Connection is returned to pool, not held for 30 seconds
                    task_db.close()

                # Process results after closing DB connection
                for task_id, task_dict, current_state in task_dicts:
                    # Only send if state changed
                    if last_states.get(task_id) != current_state:
                        last_states[task_id] = current_state

                        # Format as SSE
                        yield f"data: {json.dumps(task_dict)}\n\n"

                # Clean up old completed tasks from tracking
                current_task_ids = {task_id for task_id, _, _ in task_dicts}
                for task_id in list(last_states.keys()):
                    if task_id not in current_task_ids:
                        del last_states[task_id]

                # Send heartbeat comment every 30 seconds to keep connection alive
                if not event_occurred:
                    yield f": heartbeat\n\n"

        except asyncio.CancelledError:
            logger.info("SSE stream cancelled by client")
            raise
        except Exception as e:
            logger.error(f"Error in SSE stream: {e}", exc_info=True)
            yield f"event: error\ndata: {json.dumps({'error': str(e)})}\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache, no-transform",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",  # Disable nginx buffering
            "Content-Type": "text/event-stream",
            "Transfer-Encoding": "chunked",
            # CORS headers for SSE
            "Access-Control-Allow-Origin": "*",
            "Access-Control-Allow-Credentials": "true",
        }
    )


@router.get(
    "/training/tasks/{task_id}",
    response_model=SuccessResponse,
    summary="Get training task status",
    description="Get detailed status of a specific training task.",
)
def get_training_task_status(
    task_id: str,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[User, Depends(get_current_user)],
) -> SuccessResponse:
    """Get training task status."""
    from app.models.training_task import TrainingTask

    task = db.query(TrainingTask).filter(TrainingTask.id == task_id).first()

    if not task:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Training task {task_id} not found",
        )

    return SuccessResponse(
        success=True,
        data=task.to_dict()
    )
