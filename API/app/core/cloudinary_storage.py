"""Cloudinary storage utility for ML models."""

import cloudinary
import cloudinary.uploader
from cloudinary.utils import cloudinary_url
from typing import Dict, Optional
import os
from pathlib import Path


class CloudinaryStorage:
    """Manage ML model uploads to Cloudinary."""

    def __init__(self):
        """Initialize Cloudinary configuration from environment variables."""
        cloudinary.config(
            cloud_name=os.getenv("CLOUDINARY_CLOUD_NAME"),
            api_key=os.getenv("CLOUDINARY_API_KEY"),
            api_secret=os.getenv("CLOUDINARY_API_SECRET"),
            secure=True,
        )

    def upload_model(
        self,
        file_path: str,
        model_version: str,
        model_type: str = "tflite",
        folder: str = "aquaforecast/models",
    ) -> Dict[str, str]:
        """
        Upload ML model to Cloudinary.

        Args:
            file_path: Local path to the model file
            model_version: Version string (e.g., "1.2.0")
            model_type: Type of model ("tflite" or "keras")
            folder: Cloudinary folder path

        Returns:
            Dict containing url, public_id, and size_bytes

        Raises:
            Exception: If upload fails
        """
        # Validate file exists
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"Model file not found: {file_path}")

        # Get file extension
        file_ext = Path(file_path).suffix

        # Create public ID
        public_id = f"{folder}/v{model_version}_{model_type}{file_ext}"

        try:
            # Upload as raw file (not image/video)
            result = cloudinary.uploader.upload(
                file_path,
                resource_type="raw",  # Important: use 'raw' for non-media files
                public_id=public_id,
                overwrite=True,
                invalidate=True,  # Clear CDN cache
                tags=[model_version, model_type, "ml_model"],
            )

            # Get file size
            file_size = os.path.getsize(file_path)

            return {
                "url": result["secure_url"],
                "public_id": result["public_id"],
                "size_bytes": file_size,
                "cloudinary_id": result["public_id"],
            }

        except Exception as e:
            raise Exception(f"Failed to upload model to Cloudinary: {str(e)}")

    def generate_download_url(
        self,
        public_id: str,
        expiry_hours: int = 24,
    ) -> str:
        """
        Generate a signed download URL for a model.

        Args:
            public_id: Cloudinary public ID
            expiry_hours: URL expiry time in hours

        Returns:
            Signed URL string
        """
        from datetime import datetime, timedelta

        # Calculate expiry timestamp
        expiry_time = datetime.utcnow() + timedelta(hours=expiry_hours)
        expiry_timestamp = int(expiry_time.timestamp())

        # Generate signed URL
        url, _ = cloudinary_url(
            public_id,
            resource_type="raw",
            type="upload",
            sign_url=True,
            secure=True,
        )

        return url

    def delete_model(self, public_id: str) -> bool:
        """
        Delete a model from Cloudinary.

        Args:
            public_id: Cloudinary public ID

        Returns:
            True if deletion was successful

        Raises:
            Exception: If deletion fails
        """
        try:
            result = cloudinary.uploader.destroy(
                public_id,
                resource_type="raw",
                invalidate=True,
            )

            return result.get("result") == "ok"

        except Exception as e:
            raise Exception(f"Failed to delete model from Cloudinary: {str(e)}")

    def get_model_info(self, public_id: str) -> Optional[Dict]:
        """
        Get information about a model stored in Cloudinary.

        Args:
            public_id: Cloudinary public ID

        Returns:
            Dict with model info or None if not found
        """
        try:
            result = cloudinary.api.resource(
                public_id,
                resource_type="raw",
            )

            return {
                "url": result["secure_url"],
                "size_bytes": result["bytes"],
                "created_at": result["created_at"],
                "format": result["format"],
            }

        except Exception:
            return None


# Singleton instance
cloudinary_storage = CloudinaryStorage()
