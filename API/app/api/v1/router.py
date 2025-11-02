"""Main v1 API router combining all endpoint modules."""

from fastapi import APIRouter

from app.api.v1.endpoints import auth, farm_data, models

# Create main v1 router
api_router = APIRouter()

# Include authentication endpoints
api_router.include_router(
    auth.router,
    prefix="/auth",
    tags=["Authentication"],
)

# Include farm data endpoints
api_router.include_router(
    farm_data.router,
    prefix="/farm-data",
    tags=["Farm Data"],
)

# Include ML model endpoints
api_router.include_router(
    models.router,
    prefix="/models",
    tags=["ML Models"],
)
