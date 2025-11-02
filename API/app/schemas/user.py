"""User schemas for request/response validation."""

from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field, EmailStr


class UserCreate(BaseModel):
    """Schema for user registration request."""

    firebase_uid: str = Field(
        ...,
        min_length=1,
        max_length=128,
        description="Firebase user ID",
    )
    email: EmailStr = Field(..., description="User email address")

    class Config:
        json_schema_extra = {
            "example": {
                "firebase_uid": "abc123xyz456",
                "email": "farmer@example.com",
            }
        }


class UserResponse(BaseModel):
    """Schema for user registration response."""

    user_id: str = Field(..., description="Internal user ID (UUID)")
    firebase_uid: str = Field(..., description="Firebase user ID")
    email: str = Field(..., description="User email address")
    created_at: datetime = Field(..., description="Account creation timestamp")

    class Config:
        json_schema_extra = {
            "example": {
                "user_id": "550e8400-e29b-41d4-a716-446655440000",
                "firebase_uid": "abc123xyz456",
                "email": "farmer@example.com",
                "created_at": "2025-01-15T10:30:00Z",
            }
        }


class UserProfileResponse(BaseModel):
    """Schema for user profile response."""

    user_id: str = Field(..., description="Internal user ID (UUID)")
    firebase_uid: str = Field(..., description="Firebase user ID")
    email: str = Field(..., description="User email address")
    last_sync_at: Optional[datetime] = Field(
        default=None, description="Last data sync timestamp"
    )
    total_readings: int = Field(
        default=0, description="Total water quality readings synced"
    )

    class Config:
        json_schema_extra = {
            "example": {
                "user_id": "550e8400-e29b-41d4-a716-446655440000",
                "firebase_uid": "abc123xyz456",
                "email": "farmer@example.com",
                "last_sync_at": "2025-01-15T14:20:00Z",
                "total_readings": 342,
            }
        }
