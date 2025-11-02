"""ML model schemas."""

from datetime import datetime
from typing import Optional, Dict, Any
from pydantic import BaseModel, Field


class ModelVersionResponse(BaseModel):
    """ML model version response."""
    model_id: str
    version: str
    model_url: str
    model_size_bytes: Optional[int]
    preprocessing_config: Optional[Dict[str, Any]]
    is_active: bool
    min_app_version: Optional[str]
    created_at: datetime


class ModelUpdateCheckResponse(BaseModel):
    """Model update check response."""
    update_available: bool
    current_version: str
    latest_version: Optional[str] = None
    model_url: Optional[str] = None
    model_size_bytes: Optional[int] = None
    release_notes: Optional[str] = None
