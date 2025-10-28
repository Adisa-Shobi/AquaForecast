"""Tests for farm data endpoints."""

import pytest
from unittest.mock import patch
from datetime import datetime


class TestFarmDataEndpoints:
    """Test suite for farm data endpoints."""

    @patch("app.api.deps.firebase_auth.verify_token")
    @patch("app.api.deps.UserService.get_user_by_firebase_uid")
    def test_sync_farm_data_success(
        self, mock_get_user, mock_verify_token, client, db_session, mock_firebase_uid
    ):
        """Test successful farm data sync."""
        from app.models.user import User
        from app.services.user_service import UserService

        # Create test user
        mock_verify_token.return_value = {"uid": mock_firebase_uid}
        test_user = User(
            firebase_uid=mock_firebase_uid,
            email="test@example.com",
            is_active=True,
        )
        db_session.add(test_user)
        db_session.commit()

        mock_get_user.return_value = test_user

        # Sync request
        response = client.post(
            "/api/v1/farm-data/sync",
            headers={"Authorization": "Bearer mock_token"},
            json={
                "device_id": "test-device-123",
                "readings": [
                    {
                        "temperature": 28.5,
                        "ph": 7.2,
                        "dissolved_oxygen": 6.8,
                        "ammonia": 0.15,
                        "nitrate": 10.5,
                        "turbidity": 12.3,
                        "location": {"latitude": -1.2921, "longitude": 36.8219},
                        "country_code": "KE",
                        "recorded_at": "2025-01-15T08:00:00Z",
                    }
                ],
            },
        )

        assert response.status_code == 201
        data = response.json()
        assert data["success"] is True
        assert data["data"]["synced_count"] == 1
        assert data["data"]["failed_count"] == 0

    @patch("app.api.deps.firebase_auth.verify_token")
    @patch("app.api.deps.UserService.get_user_by_firebase_uid")
    def test_get_farm_data(
        self, mock_get_user, mock_verify_token, client, db_session, mock_firebase_uid
    ):
        """Test getting farm data."""
        from app.models.user import User

        mock_verify_token.return_value = {"uid": mock_firebase_uid}
        test_user = User(
            firebase_uid=mock_firebase_uid,
            email="test@example.com",
            is_active=True,
        )
        db_session.add(test_user)
        db_session.commit()

        mock_get_user.return_value = test_user

        # First sync some data
        client.post(
            "/api/v1/farm-data/sync",
            headers={"Authorization": "Bearer mock_token"},
            json={
                "readings": [
                    {
                        "temperature": 28.5,
                        "ph": 7.2,
                        "dissolved_oxygen": 6.8,
                        "ammonia": 0.15,
                        "nitrate": 10.5,
                        "turbidity": 12.3,
                        "location": {"latitude": -1.2921, "longitude": 36.8219},
                        "country_code": "KE",
                        "recorded_at": "2025-01-15T08:00:00Z",
                    }
                ],
            },
        )

        # Get farm data
        response = client.get(
            "/api/v1/farm-data",
            headers={"Authorization": "Bearer mock_token"},
        )

        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert data["data"]["total"] >= 1

    @patch("app.api.deps.firebase_auth.verify_token")
    @patch("app.api.deps.UserService.get_user_by_firebase_uid")
    def test_delete_user_data(
        self, mock_get_user, mock_verify_token, client, db_session, mock_firebase_uid
    ):
        """Test deleting user data."""
        from app.models.user import User

        mock_verify_token.return_value = {"uid": mock_firebase_uid}
        test_user = User(
            firebase_uid=mock_firebase_uid,
            email="test@example.com",
            is_active=True,
        )
        db_session.add(test_user)
        db_session.commit()

        mock_get_user.return_value = test_user

        # Delete data
        response = client.delete(
            "/api/v1/farm-data/user",
            headers={"Authorization": "Bearer mock_token"},
        )

        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert "deleted_count" in data["data"]

    def test_sync_without_auth(self, client):
        """Test sync without authentication fails."""
        response = client.post(
            "/api/v1/farm-data/sync",
            json={"readings": []},
        )
        assert response.status_code == 401
