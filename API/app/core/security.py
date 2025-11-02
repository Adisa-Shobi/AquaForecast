"""Security utilities including Firebase authentication."""

import os
from typing import Optional
from fastapi import HTTPException, status
import firebase_admin
from firebase_admin import credentials, auth
from app.core.config import settings


class FirebaseAuth:
    """Firebase authentication manager."""

    def __init__(self):
        """Initialize Firebase Admin SDK."""
        self._initialized = False
        self._initialize()

    def _initialize(self):
        """Initialize Firebase Admin SDK with credentials."""
        if self._initialized:
            return

        # Check if credentials file exists
        if not os.path.exists(settings.FIREBASE_CREDENTIALS_PATH):
            raise FileNotFoundError(
                f"Firebase credentials not found at: {settings.FIREBASE_CREDENTIALS_PATH}"
            )

        # Initialize Firebase Admin SDK
        cred = credentials.Certificate(settings.FIREBASE_CREDENTIALS_PATH)
        firebase_admin.initialize_app(cred)
        self._initialized = True

    def verify_token(self, token: str) -> dict:
        """
        Verify Firebase ID token.

        Args:
            token: Firebase ID token from Authorization header

        Returns:
            dict: Decoded token containing user info (uid, email, etc.)

        Raises:
            HTTPException: If token is invalid or expired
        """
        try:
            # Verify the token
            decoded_token = auth.verify_id_token(token)
            return decoded_token
        except auth.InvalidIdTokenError:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid authentication token",
            )
        except auth.ExpiredIdTokenError:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Authentication token has expired",
            )
        except auth.RevokedIdTokenError:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Authentication token has been revoked",
            )
        except Exception as e:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail=f"Authentication failed: {str(e)}",
            )

    def get_user_by_uid(self, uid: str) -> Optional[auth.UserRecord]:
        """
        Get Firebase user by UID.

        Args:
            uid: Firebase user ID

        Returns:
            UserRecord: Firebase user record or None if not found
        """
        try:
            return auth.get_user(uid)
        except auth.UserNotFoundError:
            return None
        except Exception:
            return None


# Global Firebase auth instance
firebase_auth = FirebaseAuth()
