"""Common schemas used across the API."""

from typing import Any, Optional, Dict
from pydantic import BaseModel, Field


class SuccessResponse(BaseModel):
    """Standard success response wrapper."""

    success: bool = Field(default=True, description="Whether request was successful")
    data: Any = Field(..., description="Response data")
    meta: Optional[Dict[str, Any]] = Field(
        default=None, description="Optional metadata"
    )

    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "data": {"message": "Operation completed successfully"},
                "meta": {"timestamp": "2025-01-15T10:30:00Z"},
            }
        }


class ErrorDetail(BaseModel):
    """Error detail structure."""

    code: str = Field(..., description="Error code")
    message: str = Field(..., description="Human-readable error message")
    details: Optional[Dict[str, Any]] = Field(
        default=None, description="Additional error details"
    )

    class Config:
        json_schema_extra = {
            "example": {
                "code": "AUTH_001",
                "message": "Invalid or expired token",
                "details": {"token_type": "firebase_id_token"},
            }
        }


class ErrorResponse(BaseModel):
    """Standard error response wrapper."""

    success: bool = Field(default=False, description="Always false for errors")
    error: ErrorDetail = Field(..., description="Error details")

    class Config:
        json_schema_extra = {
            "example": {
                "success": False,
                "error": {
                    "code": "AUTH_001",
                    "message": "Invalid or expired token",
                    "details": None,
                },
            }
        }


class HealthResponse(BaseModel):
    """Health check response."""

    status: str = Field(..., description="Service status")
    version: str = Field(..., description="API version")
    environment: str = Field(..., description="Environment name")

    class Config:
        json_schema_extra = {
            "example": {
                "status": "healthy",
                "version": "1.0.0",
                "environment": "development",
            }
        }
