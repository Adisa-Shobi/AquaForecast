"""Tests for authentication endpoints."""

import pytest
from unittest.mock import patch, MagicMock


class TestAuthEndpoints:
    """Test suite for authentication endpoints."""

    def test_health_check(self, client):
        """Test health check endpoint."""
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
        assert "version" in data
        assert "environment" in data

    def test_root_endpoint(self, client):
        """Test root endpoint."""
        response = client.get("/")
        assert response.status_code == 200
        data = response.json()
        assert "message" in data
        assert "version" in data

    @patch("app.api.deps.firebase_auth.verify_token")
    def test_register_user_success(
        self, mock_verify_token, client, mock_firebase_uid, mock_user_email
    ):
        """Test successful user registration."""
        # Mock Firebase token verification
        mock_verify_token.return_value = {"uid": mock_firebase_uid}

        # Registration request
        response = client.post(
            "/api/v1/auth/register",
            json={"firebase_uid": mock_firebase_uid, "email": mock_user_email},
            headers={"Authorization": f"Bearer mock_token"},
        )

        assert response.status_code == 201
        data = response.json()
        assert data["success"] is True
        assert data["data"]["email"] == mock_user_email
        assert data["data"]["firebase_uid"] == mock_firebase_uid
        assert "user_id" in data["data"]
        assert data["meta"]["created"] is True

    @patch("app.api.deps.firebase_auth.verify_token")
    def test_register_user_duplicate(
        self, mock_verify_token, client, mock_firebase_uid, mock_user_email
    ):
        """Test registering the same user twice returns existing user."""
        # Mock Firebase token verification
        mock_verify_token.return_value = {"uid": mock_firebase_uid}

        # First registration
        response1 = client.post(
            "/api/v1/auth/register",
            json={"firebase_uid": mock_firebase_uid, "email": mock_user_email},
            headers={"Authorization": f"Bearer mock_token"},
        )
        assert response1.status_code == 201

        # Second registration (should return existing user)
        response2 = client.post(
            "/api/v1/auth/register",
            json={"firebase_uid": mock_firebase_uid, "email": mock_user_email},
            headers={"Authorization": f"Bearer mock_token"},
        )
        assert response2.status_code == 201
        data = response2.json()
        assert data["success"] is True
        assert data["meta"]["created"] is False

    def test_register_user_missing_token(self, client, mock_firebase_uid, mock_user_email):
        """Test registration without authorization token fails."""
        response = client.post(
            "/api/v1/auth/register",
            json={"firebase_uid": mock_firebase_uid, "email": mock_user_email},
        )
        assert response.status_code == 401

    @patch("app.api.deps.firebase_auth.verify_token")
    def test_register_user_uid_mismatch(
        self, mock_verify_token, client, mock_firebase_uid, mock_user_email
    ):
        """Test registration with mismatched Firebase UID fails."""
        # Mock Firebase token verification with different UID
        mock_verify_token.return_value = {"uid": "different_uid"}

        response = client.post(
            "/api/v1/auth/register",
            json={"firebase_uid": mock_firebase_uid, "email": mock_user_email},
            headers={"Authorization": f"Bearer mock_token"},
        )
        assert response.status_code == 400

    @patch("app.api.deps.firebase_auth.verify_token")
    def test_register_user_invalid_email(self, mock_verify_token, client, mock_firebase_uid):
        """Test registration with invalid email fails validation."""
        mock_verify_token.return_value = {"uid": mock_firebase_uid}

        response = client.post(
            "/api/v1/auth/register",
            json={"firebase_uid": mock_firebase_uid, "email": "invalid_email"},
            headers={"Authorization": f"Bearer mock_token"},
        )
        assert response.status_code == 422

    @patch("app.api.deps.firebase_auth.verify_token")
    @patch("app.api.deps.UserService.get_user_by_firebase_uid")
    def test_get_current_user_success(
        self, mock_get_user, mock_verify_token, client, mock_firebase_uid, mock_user_email
    ):
        """Test getting current user profile."""
        # Mock Firebase token verification
        mock_verify_token.return_value = {"uid": mock_firebase_uid}

        # First register a user
        client.post(
            "/api/v1/auth/register",
            json={"firebase_uid": mock_firebase_uid, "email": mock_user_email},
            headers={"Authorization": f"Bearer mock_token"},
        )

        # Get user profile
        response = client.get(
            "/api/v1/auth/me", headers={"Authorization": f"Bearer mock_token"}
        )

        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert data["data"]["email"] == mock_user_email
        assert data["data"]["firebase_uid"] == mock_firebase_uid
        assert "total_readings" in data["data"]

    def test_get_current_user_missing_token(self, client):
        """Test getting user profile without token fails."""
        response = client.get("/api/v1/auth/me")
        assert response.status_code == 401

    @patch("app.api.deps.firebase_auth.verify_token")
    def test_get_current_user_not_registered(self, mock_verify_token, client):
        """Test getting profile for unregistered user fails."""
        mock_verify_token.return_value = {"uid": "unregistered_uid"}

        response = client.get(
            "/api/v1/auth/me", headers={"Authorization": f"Bearer mock_token"}
        )
        assert response.status_code == 404
