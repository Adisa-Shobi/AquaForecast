"""ML model version tracking."""

from datetime import datetime
from sqlalchemy import Column, String, DateTime, Boolean, BigInteger, Text, ForeignKey, Integer, Float, Enum as SQLEnum
from sqlalchemy.dialects.postgresql import UUID, JSONB, ARRAY
from sqlalchemy.orm import relationship
import uuid
import enum

from app.core.database import Base


class ModelStatus(str, enum.Enum):
    """Model status enumeration."""
    TRAINING = "training"
    COMPLETED = "completed"
    FAILED = "failed"
    DEPLOYED = "deployed"
    ARCHIVED = "archived"


class ModelVersion(Base):
    """ML model version tracking for TFLite model distribution."""

    __tablename__ = "model_versions"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    version = Column(String(50), unique=True, nullable=False, index=True)

    # Storage information - Both Keras and TFLite models
    tflite_model_url = Column(Text, nullable=False, comment="Cloudinary URL for TFLite model (.tflite)")
    keras_model_url = Column(Text, nullable=True, comment="Cloudinary URL for Keras model (.keras)")
    tflite_size_bytes = Column(BigInteger, nullable=True, comment="TFLite model size")
    keras_size_bytes = Column(BigInteger, nullable=True, comment="Keras model size")

    # Cloudinary storage IDs for management
    tflite_cloudinary_id = Column(String(255), nullable=True, comment="Cloudinary public ID for TFLite model")
    keras_cloudinary_id = Column(String(255), nullable=True, comment="Cloudinary public ID for Keras model")

    # Model metadata
    base_model_id = Column(UUID(as_uuid=True), ForeignKey("model_versions.id", ondelete="SET NULL"), nullable=True, comment="Parent model used for retraining")
    preprocessing_config = Column(JSONB, nullable=True, comment="Scaler config, feature names, biological limits")
    model_config = Column(JSONB, nullable=True, comment="Model architecture and training config")

    # Training information
    training_data_count = Column(Integer, nullable=True, comment="Number of samples used for training")
    training_duration_seconds = Column(Integer, nullable=True)
    trained_by = Column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="SET NULL"), nullable=True)

    # Metrics
    metrics = Column(JSONB, nullable=True, comment="R2, RMSE, MAE for weight and length predictions")

    # Status and deployment
    status = Column(SQLEnum(ModelStatus, values_callable=lambda obj: [e.value for e in obj]), nullable=False, default=ModelStatus.TRAINING, index=True)
    is_deployed = Column(Boolean, nullable=False, default=False, index=True, comment="Currently deployed model for production")
    is_active = Column(Boolean, nullable=False, default=True, comment="Available for download")

    # App compatibility
    min_app_version = Column(String(20), nullable=True)

    # Audit fields
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow, index=True)
    deployed_at = Column(DateTime, nullable=True)
    notes = Column(Text, nullable=True, comment="Release notes or training notes")

    # Relationships
    base_model = relationship("ModelVersion", remote_side=[id], backref="child_models")
    training_sessions = relationship("TrainingSession", back_populates="model_version", cascade="all, delete-orphan")

    def __repr__(self) -> str:
        return f"<ModelVersion(version={self.version}, status={self.status}, is_deployed={self.is_deployed})>"

    def to_dict(self) -> dict:
        """Convert model version to dictionary."""
        return {
            "model_id": str(self.id),
            "version": self.version,
            "tflite_model_url": self.tflite_model_url,
            "keras_model_url": self.keras_model_url,
            "tflite_size_bytes": self.tflite_size_bytes,
            "keras_size_bytes": self.keras_size_bytes,
            "base_model_id": str(self.base_model_id) if self.base_model_id else None,
            "base_model_version": self.base_model.version if self.base_model else None,
            "preprocessing_config": self.preprocessing_config,
            "model_config": self.model_config,
            "training_data_count": self.training_data_count,
            "training_duration_seconds": self.training_duration_seconds,
            "metrics": self.metrics,
            "status": self.status.value if self.status else None,
            "is_deployed": self.is_deployed,
            "is_active": self.is_active,
            "min_app_version": self.min_app_version,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "deployed_at": self.deployed_at.isoformat() if self.deployed_at else None,
            "notes": self.notes,
        }


class TrainingSession(Base):
    """Track training sessions and farm data usage to prevent reuse."""

    __tablename__ = "training_sessions"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    model_version_id = Column(UUID(as_uuid=True), ForeignKey("model_versions.id", ondelete="CASCADE"), nullable=False, index=True)

    # Training metadata - Track which farm data was used
    farm_data_ids = Column(ARRAY(UUID(as_uuid=True)), nullable=False, comment="IDs of farm data used for training")
    training_samples = Column(Integer, nullable=False)
    validation_samples = Column(Integer, nullable=False)
    test_samples = Column(Integer, nullable=False)

    # Training parameters
    epochs = Column(Integer, nullable=True)
    batch_size = Column(Integer, nullable=True)
    learning_rate = Column(Float, nullable=True)

    # Results
    final_metrics = Column(JSONB, nullable=True)
    training_history = Column(JSONB, nullable=True, comment="Loss and metrics per epoch")

    # Audit
    started_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    completed_at = Column(DateTime, nullable=True)

    # Relationships
    model_version = relationship("ModelVersion", back_populates="training_sessions")

    def __repr__(self) -> str:
        return f"<TrainingSession(model_version_id={self.model_version_id}, samples={self.training_samples})>"
