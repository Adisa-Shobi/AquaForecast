"""API dependencies for authentication and database access."""

from typing import Annotated
from fastapi import Depends, HTTPException, status, Header
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.security import firebase_auth
from app.models.user import User
from app.services.user_service import UserService


def get_current_user_firebase_uid(
    authorization: Annotated[str, Header(description="Bearer token from Firebase")]
) -> str:
    """
    Extract and verify Firebase ID token from Authorization header.

    Args:
        authorization: Authorization header value (Bearer <token>)

    Returns:
        Firebase UID of authenticated user

    Raises:
        HTTPException: If token is missing, invalid, or expired
    """
    if not authorization:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing authorization header",
            headers={"WWW-Authenticate": "Bearer"},
        )

    # Extract token from "Bearer <token>"
    parts = authorization.split()
    if len(parts) != 2 or parts[0].lower() != "bearer":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authorization header format. Expected: Bearer <token>",
            headers={"WWW-Authenticate": "Bearer"},
        )

    token = parts[1]

    # Verify token with Firebase
    decoded_token = firebase_auth.verify_token(token)

    # Extract Firebase UID
    firebase_uid = decoded_token.get("uid")
    if not firebase_uid:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token: missing user ID",
        )

    return firebase_uid


def get_current_user(
    db: Annotated[Session, Depends(get_db)],
    firebase_uid: Annotated[str, Depends(get_current_user_firebase_uid)],
) -> User:
    """
    Get current authenticated user from database.

    Args:
        db: Database session
        firebase_uid: Firebase UID from verified token

    Returns:
        Current user

    Raises:
        HTTPException: If user not found in database
    """
    user = UserService.get_user_by_firebase_uid(db, firebase_uid)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found. Please register first.",
        )

    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="User account is deactivated",
        )

    return user
