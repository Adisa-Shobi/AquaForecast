"""Tests for ML model endpoints."""

import pytest
from datetime import datetime


class TestModelEndpoints:
    """Test suite for ML model endpoints."""

    def test_get_latest_model_not_found(self, client):
        """Test getting latest model when none exists."""
        response = client.get("/api/v1/models/latest")
        assert response.status_code == 404

    def test_get_latest_model_success(self, client, db_session):
        """Test getting latest model successfully."""
        from app.models.model_version import ModelVersion

        # Create test model
        model = ModelVersion(
            version="v1.0.0",
            model_url="https://example.com/model.tflite",
            model_size_bytes=1024000,
            preprocessing_config={"mean": 0.5, "std": 0.2},
            is_active=True,
            min_app_version="1.0.0",
        )
        db_session.add(model)
        db_session.commit()

        response = client.get("/api/v1/models/latest")
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert data["data"]["version"] == "v1.0.0"

    def test_check_update_no_update(self, client, db_session):
        """Test check update when already on latest version."""
        from app.models.model_version import ModelVersion

        model = ModelVersion(
            version="v1.0.0",
            model_url="https://example.com/model.tflite",
            is_active=True,
        )
        db_session.add(model)
        db_session.commit()

        response = client.get(
            "/api/v1/models/check-update",
            params={"current_version": "v1.0.0"},
        )
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert data["data"]["update_available"] is False

    def test_check_update_available(self, client, db_session):
        """Test check update when new version available."""
        from app.models.model_version import ModelVersion

        model = ModelVersion(
            version="v2.0.0",
            model_url="https://example.com/model_v2.tflite",
            model_size_bytes=2048000,
            is_active=True,
        )
        db_session.add(model)
        db_session.commit()

        response = client.get(
            "/api/v1/models/check-update",
            params={"current_version": "v1.0.0"},
        )
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert data["data"]["update_available"] is True
        assert data["data"]["latest_version"] == "v2.0.0"
        assert "model_url" in data["data"]
