"""Main FastAPI application."""

import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.exceptions import RequestValidationError
from sqlalchemy.exc import SQLAlchemyError
import time

from app.core.config import settings
from app.core.database import engine, Base
from app.api.v1.router import api_router
from app.schemas.common import HealthResponse
from app.middleware.error_handler import (
    validation_exception_handler,
    database_exception_handler,
    general_exception_handler,
)

# Configure logging
logging.basicConfig(
    level=getattr(logging, settings.LOG_LEVEL),
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Application lifespan events.

    Handles startup and shutdown tasks.
    """
    # Startup
    logger.info("Starting AquaForecast API...")
    logger.info(f"Environment: {settings.ENVIRONMENT}")
    logger.info(f"Version: {settings.VERSION}")

    # Create database tables
    try:
        Base.metadata.create_all(bind=engine)
        logger.info("Database tables created successfully")
    except Exception as e:
        logger.error(f"Failed to create database tables: {e}")
        raise

    yield

    # Shutdown
    logger.info("Shutting down AquaForecast API...")


# Create FastAPI application
app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    description="""
    Privacy-first REST API for aquaculture water quality data collection and analytics.

    ## Features

    - 🔐 **Secure Authentication**: Firebase-based authentication
    - 🌊 **Water Quality Data**: Sync water parameters with user consent
    - 🔒 **Privacy-First**: Only water parameters stored, predictions stay local
    - 📊 **Analytics**: Aggregated regional water quality insights

    ## Data Privacy

    **What we collect (with consent)**:
    - ✅ Email and authentication IDs only
    - ✅ Water quality parameters (temperature, pH, DO, ammonia, nitrate, turbidity)
    - ✅ Location data for analytics grouping

    **What stays local**:
    - ❌ Farm and pond configurations
    - ❌ Predictions and feeding schedules
    - ❌ User profile information (name, photo, etc.)
    """,
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json",
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Request logging middleware
@app.middleware("http")
async def log_requests(request: Request, call_next):
    """Log all incoming requests."""
    start_time = time.time()

    # Process request
    response = await call_next(request)

    # Calculate duration
    duration = time.time() - start_time

    # Log request
    logger.info(
        f"{request.method} {request.url.path} "
        f"completed in {duration:.3f}s with status {response.status_code}"
    )

    return response


# Register exception handlers
app.add_exception_handler(RequestValidationError, validation_exception_handler)
app.add_exception_handler(SQLAlchemyError, database_exception_handler)
app.add_exception_handler(Exception, general_exception_handler)

# Include API v1 router
app.include_router(api_router, prefix=settings.API_V1_PREFIX)


# Health check endpoint
@app.get(
    "/health",
    response_model=HealthResponse,
    tags=["Health"],
    summary="Health check",
    description="Check if the API is running and healthy",
)
def health_check() -> HealthResponse:
    """
    Health check endpoint.

    Returns:
        API health status, version, and environment
    """
    return HealthResponse(
        status="healthy",
        version=settings.VERSION,
        environment=settings.ENVIRONMENT,
    )


# Root endpoint
@app.get(
    "/",
    tags=["Root"],
    summary="API root",
    description="Root endpoint with API information",
)
def read_root():
    """
    Root endpoint with API information.

    Returns:
        Welcome message and links
    """
    return {
        "message": "Welcome to AquaForecast API",
        "version": settings.VERSION,
        "docs": "/docs",
        "health": "/health",
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=settings.ENVIRONMENT == "development",
    )
