"""ML model service - model version management."""

import logging
from typing import Optional, List, Dict, Any
from sqlalchemy.orm import Session
from datetime import datetime

from app.models.model_version import ModelVersion, ModelStatus
from app.models.training_task import TrainingTask
from app.core.cloudinary_storage import cloudinary_storage

logger = logging.getLogger(__name__)


class ModelService:
    """Service for ML model version management."""

    @staticmethod
    def get_latest_model(db: Session) -> Optional[ModelVersion]:
        """
        Get the latest model version.

        Returns the deployed model if one exists, otherwise returns the latest active model.
        """
        # First check for deployed model
        deployed_model = ModelService.get_deployed_model(db)
        if deployed_model:
            return deployed_model

        # Fallback to latest active model
        return (
            db.query(ModelVersion)
            .filter(ModelVersion.is_active == True)
            .order_by(ModelVersion.created_at.desc())
            .first()
        )

    @staticmethod
    def get_deployed_model(db: Session) -> Optional[ModelVersion]:
        """
        Get the currently deployed model.

        Note: Only one model should be deployed at a time.
        This is enforced by the deploy_model() method.
        """
        return (
            db.query(ModelVersion)
            .filter(ModelVersion.is_deployed == True)
            .order_by(ModelVersion.deployed_at.desc())
            .first()
        )

    @staticmethod
    def get_all_models(db: Session, include_archived: bool = False) -> List[ModelVersion]:
        """
        Get all models.

        Args:
            db: Database session
            include_archived: Whether to include archived models

        Returns:
            List of ModelVersion objects
        """
        query = db.query(ModelVersion)

        if not include_archived:
            query = query.filter(ModelVersion.status != ModelStatus.ARCHIVED)

        return query.order_by(ModelVersion.created_at.desc()).all()

    @staticmethod
    def get_model_by_id(db: Session, model_id: str) -> Optional[ModelVersion]:
        """Get model by ID."""
        return db.query(ModelVersion).filter(ModelVersion.id == model_id).first()

    @staticmethod
    def get_model_by_version(db: Session, version: str) -> Optional[ModelVersion]:
        """Get model by version string."""
        return (
            db.query(ModelVersion)
            .filter(ModelVersion.version == version)
            .first()
        )

    @staticmethod
    def deploy_model(db: Session, model_id: str, notes: Optional[str] = None) -> ModelVersion:
        """
        Deploy a model to production.

        IMPORTANT: Only one model can be deployed at a time.
        Deploying a new model will automatically undeploy the current one.

        Args:
            db: Database session
            model_id: ID of model to deploy
            notes: Optional deployment notes

        Returns:
            Deployed ModelVersion

        Raises:
            ValueError: If model not found or not deployable
        """
        # Get model to deploy
        model = ModelService.get_model_by_id(db, model_id)
        if not model:
            raise ValueError(f"Model {model_id} not found")

        if model.status != ModelStatus.COMPLETED:
            raise ValueError(f"Can only deploy completed models, got status: {model.status}")

        if model.is_deployed:
            logger.info(f"Model {model_id} (v{model.version}) is already deployed")
            return model

        # Unset ALL currently deployed models (enforce single deployment)
        current_deployed_models = db.query(ModelVersion).filter(
            ModelVersion.is_deployed == True
        ).all()

        for deployed in current_deployed_models:
            logger.info(
                f"Undeploying model {deployed.id} (v{deployed.version}) "
                f"to deploy model {model_id} (v{model.version})"
            )
            deployed.is_deployed = False
            deployed.status = ModelStatus.COMPLETED

        # Deploy new model
        model.is_deployed = True
        model.status = ModelStatus.DEPLOYED
        model.deployed_at = datetime.utcnow()
        if notes:
            model.notes = f"{model.notes}\n\nDeployment: {notes}" if model.notes else f"Deployment: {notes}"

        db.commit()
        db.refresh(model)

        logger.info(f"Successfully deployed model {model_id} (v{model.version})")
        return model

    @staticmethod
    def undeploy_model(db: Session, model_id: str) -> ModelVersion:
        """
        Undeploy a model from production.

        Args:
            db: Database session
            model_id: ID of model to undeploy

        Returns:
            Undeployed ModelVersion

        Raises:
            ValueError: If model not found or not deployed
        """
        model = ModelService.get_model_by_id(db, model_id)
        if not model:
            raise ValueError(f"Model {model_id} not found")

        if not model.is_deployed:
            logger.info(f"Model {model_id} (v{model.version}) is not currently deployed")
            return model

        # Undeploy the model
        model.is_deployed = False
        model.status = ModelStatus.COMPLETED

        db.commit()
        db.refresh(model)

        logger.info(f"Successfully undeployed model {model_id} (v{model.version})")
        return model

    @staticmethod
    def archive_model(db: Session, model_id: str) -> ModelVersion:
        """
        Archive a model.

        Args:
            db: Database session
            model_id: ID of model to archive

        Returns:
            Archived ModelVersion

        Raises:
            ValueError: If model not found or is currently deployed
        """
        model = ModelService.get_model_by_id(db, model_id)
        if not model:
            raise ValueError(f"Model {model_id} not found")

        if model.is_deployed:
            raise ValueError("Cannot archive currently deployed model")

        model.status = ModelStatus.ARCHIVED
        model.is_active = False

        db.commit()
        db.refresh(model)

        return model

    @staticmethod
    def check_for_update(
        db: Session, current_version: str
    ) -> tuple[bool, Optional[ModelVersion]]:
        """
        Check if a newer model version is available.

        Args:
            db: Database session
            current_version: Current model version string

        Returns:
            Tuple of (update_available, latest_model)
        """
        latest_model = ModelService.get_deployed_model(db)

        # If no deployed model, check for latest active
        if not latest_model:
            latest_model = ModelService.get_latest_model(db)

        if not latest_model:
            return False, None

        # Simple version comparison (assuming semantic versioning)
        update_available = latest_model.version != current_version

        return update_available, latest_model

    @staticmethod
    def is_version_compatible(
        model: ModelVersion, app_version: Optional[str]
    ) -> bool:
        """
        Check if app version is compatible with model.

        Args:
            model: Model version
            app_version: App version string

        Returns:
            True if compatible or no min version specified
        """
        if not model.min_app_version or not app_version:
            return True

        # Simple comparison (in production, use proper version comparison)
        return app_version >= model.min_app_version

    @staticmethod
    def delete_model(db: Session, model_id: str, delete_from_storage: bool = True) -> Dict[str, Any]:
        """
        Delete a model and all its related data.

        This will delete:
        - The model version record
        - All training sessions (CASCADE)
        - Related training tasks that resulted in this model
        - Cloud storage files (TFLite, Keras, scaler)

        Args:
            db: Database session
            model_id: ID of model to delete
            delete_from_storage: Whether to delete files from cloud storage (default True)

        Returns:
            Dict with deletion summary

        Raises:
            ValueError: If model not found or is currently deployed
        """
        model = ModelService.get_model_by_id(db, model_id)
        if not model:
            raise ValueError(f"Model {model_id} not found")

        if model.is_deployed:
            raise ValueError(
                f"Cannot delete deployed model {model.version}. "
                "Please undeploy it first."
            )

        # Collect info for summary
        version = model.version
        training_sessions_count = len(model.training_sessions) if model.training_sessions else 0

        # Delete training tasks that resulted in this model
        training_tasks = db.query(TrainingTask).filter(
            TrainingTask.result_model_id == model_id
        ).all()
        training_tasks_count = len(training_tasks)

        for task in training_tasks:
            db.delete(task)
            logger.info(f"Deleted training task {task.id} for model {model_id}")

        # Delete from cloud storage
        storage_deleted = []
        storage_errors = []

        if delete_from_storage:
            # Delete TFLite model
            if model.tflite_cloudinary_id:
                try:
                    cloudinary_storage.delete_model(model.tflite_cloudinary_id)
                    storage_deleted.append(f"tflite: {model.tflite_cloudinary_id}")
                    logger.info(f"Deleted TFLite model from storage: {model.tflite_cloudinary_id}")
                except Exception as e:
                    error_msg = f"tflite: {str(e)}"
                    storage_errors.append(error_msg)
                    logger.error(f"Failed to delete TFLite model: {e}")

            # Delete Keras model
            if model.keras_cloudinary_id:
                try:
                    cloudinary_storage.delete_model(model.keras_cloudinary_id)
                    storage_deleted.append(f"keras: {model.keras_cloudinary_id}")
                    logger.info(f"Deleted Keras model from storage: {model.keras_cloudinary_id}")
                except Exception as e:
                    error_msg = f"keras: {str(e)}"
                    storage_errors.append(error_msg)
                    logger.error(f"Failed to delete Keras model: {e}")

            # Delete scaler if it exists in preprocessing_config
            if model.preprocessing_config and 'scaler_cloudinary_id' in model.preprocessing_config:
                scaler_id = model.preprocessing_config['scaler_cloudinary_id']
                try:
                    cloudinary_storage.delete_model(scaler_id)
                    storage_deleted.append(f"scaler: {scaler_id}")
                    logger.info(f"Deleted scaler from storage: {scaler_id}")
                except Exception as e:
                    error_msg = f"scaler: {str(e)}"
                    storage_errors.append(error_msg)
                    logger.error(f"Failed to delete scaler: {e}")

        # Delete the model (CASCADE will handle training_sessions)
        db.delete(model)
        db.commit()

        logger.info(
            f"Deleted model {model_id} (v{version}) with {training_sessions_count} sessions "
            f"and {training_tasks_count} tasks"
        )

        return {
            'model_id': model_id,
            'version': version,
            'deleted_training_sessions': training_sessions_count,
            'deleted_training_tasks': training_tasks_count,
            'storage_files_deleted': storage_deleted,
            'storage_errors': storage_errors if storage_errors else None,
        }

    @staticmethod
    def get_model_metrics(db: Session, model_id: str) -> dict:
        """
        Get detailed metrics for a model.

        Args:
            db: Database session
            model_id: Model ID

        Returns:
            Dict with metrics and training info

        Raises:
            ValueError: If model not found
        """
        model = ModelService.get_model_by_id(db, model_id)
        if not model:
            raise ValueError(f"Model {model_id} not found")

        # Get training session for detailed metrics
        training_session = None
        if model.training_sessions:
            training_session = model.training_sessions[0]  # Get first/latest session

        return {
            'model_id': str(model.id),
            'version': model.version,
            'metrics': model.metrics,
            'training_data_count': model.training_data_count,
            'training_duration_seconds': model.training_duration_seconds,
            'training_history': training_session.training_history if training_session else None,
            'training_samples': training_session.training_samples if training_session else None,
            'validation_samples': training_session.validation_samples if training_session else None,
            'test_samples': training_session.test_samples if training_session else None,
            'created_at': model.created_at,
        }
