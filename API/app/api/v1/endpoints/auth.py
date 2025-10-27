"""Authentication endpoints."""

from typing import Annotated
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError

from app.core.database import get_db
from app.api.deps import get_current_user_firebase_uid, get_current_user
from app.models.user import User
from app.schemas.user import UserCreate, UserResponse, UserProfileResponse
from app.schemas.common import SuccessResponse, ErrorResponse
from app.services.user_service import UserService

router = APIRouter()


@router.post(
    "/register",
    response_model=SuccessResponse,
    status_code=status.HTTP_201_CREATED,
    responses={
        201: {
            "description": "User registered successfully",
            "model": SuccessResponse,
        },
        400: {
            "description": "Invalid request data",
            "model": ErrorResponse,
        },
        401: {
            "description": "Invalid or expired authentication token",
            "model": ErrorResponse,
        },
        409: {
            "description": "User already exists",
            "model": ErrorResponse,
        },
    },
    summary="Register or verify user",
    description="""
    Register a new user or verify existing user after Firebase authentication.

    **Privacy Notice**: Only Firebase UID and email address are stored.
    No profile information (name, photo, phone) is collected.

    **Authentication**: Requires valid Firebase ID token in Authorization header.
    """,
)
def register_user(
    user_data: UserCreate,
    db: Annotated[Session, Depends(get_db)],
    firebase_uid: Annotated[str, Depends(get_current_user_firebase_uid)],
) -> SuccessResponse:
    """
    Register or verify user in the system.

    Args:
        user_data: User registration data (firebase_uid, email)
        db: Database session
        firebase_uid: Verified Firebase UID from token

    Returns:
        Success response with user data

    Raises:
        HTTPException: If firebase_uid mismatch or database error
    """
    # Verify that firebase_uid in request matches the authenticated user
    if user_data.firebase_uid != firebase_uid:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Firebase UID mismatch. Token UID does not match request data.",
        )

    try:
        # Get or create user
        user, created = UserService.get_or_create_user(db, user_data)

        # Prepare response
        user_response = UserResponse(
            user_id=str(user.id),
            firebase_uid=user.firebase_uid,
            email=user.email,
            created_at=user.created_at,
        )

        return SuccessResponse(
            success=True,
            data=user_response.model_dump(),
            meta={
                "created": created,
                "message": "User registered successfully"
                if created
                else "User already exists",
            },
        )

    except IntegrityError as e:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="User with this email or Firebase UID already exists",
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to register user: {str(e)}",
        )


@router.get(
    "/me",
    response_model=SuccessResponse,
    status_code=status.HTTP_200_OK,
    responses={
        200: {
            "description": "User profile retrieved successfully",
            "model": SuccessResponse,
        },
        401: {
            "description": "Invalid or expired authentication token",
            "model": ErrorResponse,
        },
        404: {
            "description": "User not found",
            "model": ErrorResponse,
        },
    },
    summary="Get current user profile",
    description="""
    Get current authenticated user information.

    Returns only essential data: user ID, Firebase UID, email, and sync statistics.

    **Authentication**: Requires valid Firebase ID token in Authorization header.
    """,
)
def get_current_user_profile(
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
) -> SuccessResponse:
    """
    Get current user's profile information.

    Args:
        current_user: Authenticated user from token
        db: Database session

    Returns:
        Success response with user profile data
    """
    # Get user's reading count
    total_readings = UserService.get_user_reading_count(db, str(current_user.id))

    # Prepare response
    profile_response = UserProfileResponse(
        user_id=str(current_user.id),
        firebase_uid=current_user.firebase_uid,
        email=current_user.email,
        last_sync_at=current_user.last_sync_at,
        total_readings=total_readings,
    )

    return SuccessResponse(
        success=True,
        data=profile_response.model_dump(),
    )
