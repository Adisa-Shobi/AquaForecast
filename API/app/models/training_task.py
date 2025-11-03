from datetime import datetime
from sqlalchemy import Column, String, DateTime, Integer, Float, Text, Enum as SQLEnum
from sqlalchemy.dialects.postgresql import UUID, JSONB
import uuid
import enum

from app.core.database import Base


class TrainingTaskStatus(str, enum.Enum):
    PENDING = "pending"
    RUNNING = "running"
    COMPLETED = "completed"
    FAILED = "failed"


class TrainingTask(Base):
    __tablename__ = "training_tasks"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)

    base_model_id = Column(UUID(as_uuid=True), nullable=True)
    new_version = Column(String(50), nullable=False)
    initiated_by = Column(UUID(as_uuid=True), nullable=False)

    status = Column(
        SQLEnum(TrainingTaskStatus, values_callable=lambda obj: [e.value for e in obj]),
        nullable=False,
        default=TrainingTaskStatus.PENDING,
        index=True
    )

    progress_percentage = Column(Float, default=0.0)
    current_epoch = Column(Integer, nullable=True)
    total_epochs = Column(Integer, nullable=True)
    current_stage = Column(String(100), nullable=True)

    error_message = Column(Text, nullable=True)
    result_model_id = Column(UUID(as_uuid=True), nullable=True)

    training_params = Column(JSONB, nullable=True)

    created_at = Column(DateTime, nullable=False, default=datetime.utcnow, index=True)
    started_at = Column(DateTime, nullable=True)
    completed_at = Column(DateTime, nullable=True)

    def to_dict(self) -> dict:
        return {
            "task_id": str(self.id),
            "base_model_id": str(self.base_model_id) if self.base_model_id else None,
            "new_version": self.new_version,
            "status": self.status.value,
            "progress_percentage": self.progress_percentage,
            "current_epoch": self.current_epoch,
            "total_epochs": self.total_epochs,
            "current_stage": self.current_stage,
            "error_message": self.error_message,
            "result_model_id": str(self.result_model_id) if self.result_model_id else None,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "started_at": self.started_at.isoformat() if self.started_at else None,
            "completed_at": self.completed_at.isoformat() if self.completed_at else None,
        }
