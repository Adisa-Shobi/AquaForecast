"""Application startup tasks."""

from sqlalchemy.orm import Session
from app.core.database import SessionLocal
from app.models.model_version import ModelVersion
import uuid
from datetime import datetime
import logging

logger = logging.getLogger(__name__)


def create_default_baseline_model() -> bool:
    """
    Create a default baseline model configuration.

    This is an untrained model configuration that serves as the starting point
    for training the first real model. It contains the default architecture
    and preprocessing configuration but no actual model files.

    Returns:
        True if default model was created or already exists
    """
    # Use context manager to ensure connection is always closed
    db: Session = SessionLocal()
    try:
        default_version = "0.0.0-default"
        existing = db.query(ModelVersion).filter(ModelVersion.version == default_version).first()

        if existing:
            logger.info(f"Default baseline model already exists (ID: {existing.id})")
            return True

        logger.info("Creating default baseline model configuration...")

        default_model = ModelVersion(
            id=uuid.uuid4(),
            version=default_version,
            tflite_model_url="",  # No TFLite for default
            keras_model_url=None,
            tflite_size_bytes=None,
            keras_size_bytes=None,
            tflite_cloudinary_id=None,
            keras_cloudinary_id=None,
            base_model_id=None,
            preprocessing_config={
                "feature_names": [
                    "temperature", "ph", "dissolved_oxygen",
                    "ammonia", "nitrate", "turbidity",
                    "days_in_farm", "day_of_year", "hour",
                    "sin_hour", "cos_hour", "temp_do_interaction",
                    "avg_do_7d", "avg_wqi_7d"
                ],
                "target_columns": ["fish_weight", "fish_length"],
                "optimal_do": 6.0,
                "rolling_window": 7,
                "local_scaler_path": "app/ml_models/default/default_scaler.pkl",
                "local_keras_path": "app/ml_models/default/baseline_model.keras"
            },
            model_config={
                "architecture": "Sequential",
                "input_features": 14,
                "output_features": 2,
                "layers": [
                    {"type": "Dense", "units": 64, "activation": "LeakyReLU", "l2": 0.06},
                    {"type": "BatchNormalization"},
                    {"type": "Dropout", "rate": 0.08},
                    {"type": "Dense", "units": 32, "activation": "LeakyReLU", "l2": 0.06},
                    {"type": "BatchNormalization"},
                    {"type": "Dropout", "rate": 0.08},
                    {"type": "Dense", "units": 16, "activation": "LeakyReLU"},
                    {"type": "Dense", "units": 2, "activation": "softplus"}
                ],
                "optimizer": "Adam",
                "learning_rate": 0.000006,
                "loss": "mse"
            },
            training_data_count=None,
            training_duration_seconds=None,
            trained_by=None,
            metrics=None,
            status="completed",  # Marked as completed so it can be used as base
            is_deployed=False,
            is_active=False,  # Not active for download since it has no model files
            min_app_version=None,
            created_at=datetime.utcnow(),
            deployed_at=None,
            notes="Default baseline configuration for initial model training"
        )

        db.add(default_model)
        db.commit()
        db.refresh(default_model)

        logger.info(f"Default baseline model created successfully (ID: {default_model.id})")
        logger.info("This model can be used as the base for training your first real model")

        return True

    except Exception as e:
        db.rollback()
        logger.error(f"Failed to create default baseline model: {e}", exc_info=True)
        return False
    finally:
        db.close()


def run_startup_tasks():
    """Run all startup tasks."""
    logger.info("Running startup tasks...")

    create_default_baseline_model()

    logger.info("Startup tasks completed")
