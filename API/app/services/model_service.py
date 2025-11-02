"""ML model service - model version management."""

from typing import Optional
from sqlalchemy.orm import Session

from app.models.model_version import ModelVersion


class ModelService:
    """Service for ML model version management."""

    @staticmethod
    def get_latest_model(db: Session) -> Optional[ModelVersion]:
        """Get the latest active model version."""
        return (
            db.query(ModelVersion)
            .filter(ModelVersion.is_active == True)
            .order_by(ModelVersion.created_at.desc())
            .first()
        )

    @staticmethod
    def get_model_by_version(db: Session, version: str) -> Optional[ModelVersion]:
        """Get model by version string."""
        return (
            db.query(ModelVersion)
            .filter(ModelVersion.version == version)
            .first()
        )

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
