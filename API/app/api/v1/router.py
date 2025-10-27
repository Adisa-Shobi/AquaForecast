"""Main v1 API router combining all endpoint modules."""

from fastapi import APIRouter

from app.api.v1.endpoints import auth

# Create main v1 router
api_router = APIRouter()

# Include authentication endpoints
api_router.include_router(
    auth.router,
    prefix="/auth",
    tags=["Authentication"],
)
