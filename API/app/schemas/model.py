"""ML model schemas."""

from datetime import datetime
from typing import Optional, Dict, Any, List
from pydantic import BaseModel, Field, ConfigDict
from enum import Enum


class ModelStatusEnum(str, Enum):
    """Model status enumeration."""
    TRAINING = "training"
    COMPLETED = "completed"
    FAILED = "failed"
    DEPLOYED = "deployed"
    ARCHIVED = "archived"


class ModelVersionResponse(BaseModel):
    """ML model version response."""
    model_config = ConfigDict(protected_namespaces=())

    model_id: str
    version: str
    tflite_model_url: str = Field(..., description="Cloudinary URL for TFLite model")
    keras_model_url: Optional[str] = Field(None, description="Cloudinary URL for Keras model")
    tflite_size_bytes: Optional[int]
    keras_size_bytes: Optional[int]
    base_model_id: Optional[str] = None
    base_model_version: Optional[str] = None
    preprocessing_config: Optional[Dict[str, Any]]
    model_config_data: Optional[Dict[str, Any]] = Field(None, alias="model_config")
    training_data_count: Optional[int]
    training_duration_seconds: Optional[int]
    metrics: Optional[Dict[str, Any]] = Field(None, description="R2, RMSE, MAE metrics")
    status: ModelStatusEnum
    is_deployed: bool
    is_active: bool
    min_app_version: Optional[str]
    created_at: datetime
    deployed_at: Optional[datetime]
    notes: Optional[str]


class ModelListResponse(BaseModel):
    """Response for listing all models."""
    model_config = ConfigDict(protected_namespaces=())

    models: List[ModelVersionResponse]
    total_count: int
    deployed_model_id: Optional[str] = None


class ModelUpdateCheckResponse(BaseModel):
    """Model update check response."""
    model_config = ConfigDict(protected_namespaces=())

    update_available: bool
    current_version: str
    latest_version: Optional[str] = None
    model_url: Optional[str] = None
    model_size_bytes: Optional[int] = None
    release_notes: Optional[str] = None


class RetrainRequest(BaseModel):
    """Request to retrain a model."""
    model_config = ConfigDict(protected_namespaces=())

    base_model_id: str = Field(..., description="ID of the base model to retrain from")
    new_version: str = Field(..., description="Version string for the new model (e.g., '1.2.0')")
    notes: Optional[str] = Field(None, description="Training notes or release notes")
    epochs: Optional[int] = Field(100, description="Number of training epochs", ge=1, le=500)
    batch_size: Optional[int] = Field(32, description="Training batch size", ge=8, le=256)
    learning_rate: Optional[float] = Field(0.000006, description="Learning rate", gt=0, lt=1)


class DeployModelRequest(BaseModel):
    """Request to deploy a model."""
    model_config = ConfigDict(protected_namespaces=())

    model_id: str = Field(..., description="ID of the model to deploy")
    notes: Optional[str] = Field(None, description="Deployment notes")


class ModelMetricsResponse(BaseModel):
    """Detailed model metrics response."""
    model_config = ConfigDict(protected_namespaces=())

    model_id: str
    version: str
    metrics: Dict[str, Any]
    training_data_count: int
    training_duration_seconds: Optional[int]
    training_history: Optional[Dict[str, Any]] = None
    created_at: datetime


class TrainingStatusResponse(BaseModel):
    """Training status response."""
    model_config = ConfigDict(protected_namespaces=())

    model_id: str
    version: str
    status: ModelStatusEnum
    progress: Optional[float] = Field(None, description="Training progress percentage", ge=0, le=100)
    current_epoch: Optional[int] = None
    total_epochs: Optional[int] = None
    message: Optional[str] = None
