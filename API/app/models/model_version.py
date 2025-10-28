"""ML model version tracking."""

from datetime import datetime
from sqlalchemy import Column, String, DateTime, Boolean, BigInteger, Text
from sqlalchemy.dialects.postgresql import UUID, JSONB
import uuid

from app.core.database import Base


class ModelVersion(Base):
    """ML model version tracking for TFLite model distribution."""

    __tablename__ = "model_versions"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    version = Column(String(50), unique=True, nullable=False, index=True)
    model_url = Column(Text, nullable=False)
    model_size_bytes = Column(BigInteger, nullable=True)
    preprocessing_config = Column(JSONB, nullable=True)
    is_active = Column(Boolean, nullable=False, default=True)
    min_app_version = Column(String(20), nullable=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    def __repr__(self) -> str:
        return f"<ModelVersion(version={self.version}, is_active={self.is_active})>"

    def to_dict(self) -> dict:
        """Convert model version to dictionary."""
        return {
            "model_id": str(self.id),
            "version": self.version,
            "model_url": self.model_url,
            "model_size_bytes": self.model_size_bytes,
            "preprocessing_config": self.preprocessing_config,
            "is_active": self.is_active,
            "min_app_version": self.min_app_version,
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }
