"""Application configuration using Pydantic settings."""

from typing import List
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    # Environment
    ENVIRONMENT: str = Field(default="development", description="Environment name")

    # Project Info
    PROJECT_NAME: str = Field(default="AquaForecast API", description="Project name")
    VERSION: str = Field(default="1.0.0", description="API version")
    API_V1_PREFIX: str = Field(default="/api/v1", description="API v1 prefix")

    # Database
    DATABASE_URL: str = Field(
        ..., description="PostgreSQL connection string with PostGIS"
    )

    # Firebase
    FIREBASE_CREDENTIALS_PATH: str = Field(
        default="./firebase-credentials.json",
        description="Path to Firebase Admin SDK credentials",
    )

    # CORS
    CORS_ORIGINS: str = Field(
        default="http://localhost:3000",
        description="Comma-separated list of allowed CORS origins",
    )

    @property
    def cors_origins_list(self) -> List[str]:
        """Parse CORS origins from comma-separated string."""
        return [origin.strip() for origin in self.CORS_ORIGINS.split(",")]

    # Security
    SECRET_KEY: str = Field(
        default="change-this-in-production",
        description="Secret key for signing tokens",
    )

    # Logging
    LOG_LEVEL: str = Field(default="INFO", description="Logging level")

    # Server
    HOST: str = Field(default="0.0.0.0", description="Server host")
    PORT: int = Field(default=8000, description="Server port")

    # Cloudinary
    CLOUDINARY_CLOUD_NAME: str = Field(default="", description="Cloudinary cloud name")
    CLOUDINARY_API_KEY: str = Field(default="", description="Cloudinary API key")
    CLOUDINARY_API_SECRET: str = Field(default="", description="Cloudinary API secret")

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=True,
        extra="ignore",
    )


# Global settings instance
settings = Settings()
