# AquaForecast API - Architecture Documentation

## Overview

The AquaForecast API follows **Clean Architecture** principles with clear separation of concerns, making it maintainable, testable, and scalable.

## Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                      Presentation Layer                      │
│  ┌────────────────────────────────────────────────────────┐ │
│  │            FastAPI Routes (app/api/)                   │ │
│  │  - HTTP request/response handling                      │ │
│  │  - Input validation (Pydantic)                         │ │
│  │  - Authentication middleware                           │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                       Business Layer                         │
│  ┌────────────────────────────────────────────────────────┐ │
│  │           Services (app/services/)                     │ │
│  │  - Business logic                                      │ │
│  │  - Transaction management                              │ │
│  │  - Data orchestration                                  │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      Data Access Layer                       │
│  ┌────────────────────────────────────────────────────────┐ │
│  │          Models (app/models/)                          │ │
│  │  - SQLAlchemy ORM models                               │ │
│  │  - Database schema definition                          │ │
│  │  - Relationships and constraints                       │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                         Database                             │
│                  PostgreSQL + PostGIS                        │
└─────────────────────────────────────────────────────────────┘
```

## Directory Structure

```
API/
├── app/
│   ├── main.py                    # Application entry point
│   │
│   ├── core/                      # Core utilities and configuration
│   │   ├── config.py             # Environment configuration
│   │   ├── database.py           # Database connection setup
│   │   └── security.py           # Firebase authentication
│   │
│   ├── models/                    # Database models (ORM)
│   │   └── user.py               # User model
│   │
│   ├── schemas/                   # Pydantic schemas (validation)
│   │   ├── common.py             # Common response schemas
│   │   └── user.py               # User request/response schemas
│   │
│   ├── services/                  # Business logic
│   │   └── user_service.py       # User-related operations
│   │
│   ├── api/                       # API routes
│   │   ├── deps.py               # Dependency injection
│   │   └── v1/
│   │       ├── router.py         # Main v1 router
│   │       └── endpoints/
│   │           └── auth.py       # Authentication endpoints
│   │
│   └── middleware/                # Custom middleware
│       └── error_handler.py      # Global error handling
│
├── alembic/                       # Database migrations
│   ├── versions/                 # Migration scripts
│   └── env.py                    # Alembic configuration
│
├── tests/                         # Test suite
│   ├── conftest.py               # Test fixtures
│   └── test_auth.py              # Authentication tests
│
├── requirements.txt               # Python dependencies
├── .env.example                  # Environment template
├── alembic.ini                   # Alembic config
├── Dockerfile                    # Docker image definition
├── docker-compose.yml            # Multi-container setup
└── README.md                     # Main documentation
```

## Component Responsibilities

### 1. Core (`app/core/`)

**Purpose**: Infrastructure and cross-cutting concerns

- **config.py**: Environment variables and application settings
- **database.py**: Database engine, session factory, and connection management
- **security.py**: Firebase authentication integration and token verification

**Key Principles**:
- No business logic
- Configuration loaded from environment
- Reusable across the application

### 2. Models (`app/models/`)

**Purpose**: Database schema definition using SQLAlchemy ORM

**Responsibilities**:
- Define table structure
- Specify relationships
- Set constraints and indexes
- Provide helper methods (e.g., `to_dict()`)

**Example**:
```python
class User(Base):
    __tablename__ = "users"
    id = Column(UUID, primary_key=True)
    firebase_uid = Column(String(128), unique=True)
    email = Column(String(255), unique=True)
```

**Key Principles**:
- One model per table
- Minimal business logic
- Clear naming conventions

### 3. Schemas (`app/schemas/`)

**Purpose**: Request validation and response serialization using Pydantic

**Responsibilities**:
- Validate incoming data
- Define API contract
- Serialize response data
- Document with examples

**Example**:
```python
class UserCreate(BaseModel):
    firebase_uid: str
    email: EmailStr
```

**Key Principles**:
- Separate schemas for requests and responses
- Use type hints and validation
- Include examples for documentation

### 4. Services (`app/services/`)

**Purpose**: Business logic implementation

**Responsibilities**:
- Orchestrate database operations
- Implement business rules
- Handle transactions
- Coordinate multiple models

**Example**:
```python
class UserService:
    @staticmethod
    def get_or_create_user(db: Session, user_data: UserCreate):
        # Business logic here
        pass
```

**Key Principles**:
- Pure business logic, no HTTP concerns
- Database-agnostic where possible
- Reusable across endpoints
- Testable in isolation

### 5. API (`app/api/`)

**Purpose**: HTTP request/response handling

**Responsibilities**:
- Route definitions
- Dependency injection
- HTTP status codes
- Response formatting

**Example**:
```python
@router.post("/register")
def register_user(
    user_data: UserCreate,
    db: Session = Depends(get_db)
):
    return UserService.create_user(db, user_data)
```

**Key Principles**:
- Thin controllers
- Delegate to services
- Use dependency injection
- Handle HTTP concerns only

### 6. Middleware (`app/middleware/`)

**Purpose**: Cross-cutting concerns

**Responsibilities**:
- Error handling
- Request logging
- Response formatting
- CORS configuration

**Key Principles**:
- Applied globally
- No business logic
- Consistent error responses

## Data Flow

### Request Flow Example: User Registration

```
1. Client Request
   ↓
2. FastAPI Router (/api/v1/auth/register)
   ↓
3. Pydantic Validation (UserCreate schema)
   ↓
4. Authentication Middleware (verify Firebase token)
   ↓
5. Dependency Injection (get_db, get_current_user_firebase_uid)
   ↓
6. Endpoint Handler (auth.register_user)
   ↓
7. Service Layer (UserService.get_or_create_user)
   ↓
8. Data Access (SQLAlchemy User model)
   ↓
9. Database (PostgreSQL)
   ↓
10. Response Serialization (UserResponse schema)
    ↓
11. Success Response (SuccessResponse wrapper)
    ↓
12. Client Response
```

## Authentication Flow

```
┌────────────┐
│   Client   │
└──────┬─────┘
       │ 1. Request with Firebase token
       │    Authorization: Bearer <token>
       ↓
┌──────────────────────────┐
│  FastAPI Middleware      │
│  (Authorization header)  │
└──────┬───────────────────┘
       │ 2. Extract token
       ↓
┌──────────────────────────┐
│  firebase_auth.verify()  │
│  (Firebase Admin SDK)    │
└──────┬───────────────────┘
       │ 3. Verify with Firebase
       ↓
┌──────────────────────────┐
│  Return decoded token    │
│  {uid, email, ...}       │
└──────┬───────────────────┘
       │ 4. Extract firebase_uid
       ↓
┌──────────────────────────┐
│  UserService.get_user()  │
│  (Database lookup)       │
└──────┬───────────────────┘
       │ 5. Return User object
       ↓
┌──────────────────────────┐
│  Endpoint Handler        │
│  (current_user available)│
└──────┬───────────────────┘
       │ 6. Process request
       ↓
┌────────────┐
│  Response  │
└────────────┘
```

## Database Design

### Current Schema (v1.0)

```sql
-- Users table (minimal data per privacy requirements)
CREATE TABLE users (
    id UUID PRIMARY KEY,
    firebase_uid VARCHAR(128) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_sync_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- Indexes
CREATE INDEX idx_users_firebase_uid ON users(firebase_uid);
CREATE INDEX idx_users_email ON users(email);
```

### Future Schema (v1.1+)

```sql
-- Farm data table (water quality measurements)
CREATE TABLE farm_data (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    temperature DECIMAL(5, 2),
    ph DECIMAL(4, 2),
    dissolved_oxygen DECIMAL(5, 2),
    ammonia DECIMAL(5, 2),
    nitrate DECIMAL(5, 2),
    turbidity DECIMAL(6, 2),
    location GEOGRAPHY(POINT, 4326),
    country_code VARCHAR(2),
    recorded_at TIMESTAMP,
    synced_at TIMESTAMP
);
```

## Error Handling Strategy

### Error Response Format

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable message",
    "details": { /* optional */ }
  }
}
```

### Error Codes

- **AUTH_001**: Invalid or expired token
- **VALIDATION_001**: Invalid request data
- **RESOURCE_001**: Resource not found
- **DATABASE_001**: Database operation failed

## Testing Strategy

### Test Pyramid

```
        /\
       /  \      E2E Tests (few)
      /----\
     /      \    Integration Tests (some)
    /--------\
   /          \  Unit Tests (many)
  /____________\
```

### Test Coverage

- **Unit Tests**: Services, utilities
- **Integration Tests**: API endpoints with test database
- **Fixtures**: Reusable test data and mocks

## Security Considerations

1. **Authentication**: Firebase ID token validation
2. **Authorization**: User-level access control
3. **Input Validation**: Pydantic schemas
4. **SQL Injection**: SQLAlchemy ORM (parameterized queries)
5. **CORS**: Configured allowed origins
6. **HTTPS**: Required in production
7. **Rate Limiting**: TODO (future implementation)

## Deployment Architecture

```
┌─────────────────────────────────────────┐
│         Load Balancer (nginx)           │
│         SSL Termination                 │
└────────────┬────────────────────────────┘
             │
    ┌────────┴────────┐
    │                 │
┌───▼────┐       ┌───▼────┐
│  API   │       │  API   │
│Instance│       │Instance│
│   1    │       │   2    │
└───┬────┘       └───┬────┘
    │                │
    └────────┬───────┘
             │
    ┌────────▼────────┐
    │   PostgreSQL    │
    │   + PostGIS     │
    └─────────────────┘
```

## Performance Considerations

1. **Database Connection Pooling**: SQLAlchemy connection pool
2. **Indexes**: On frequently queried fields
3. **Async Support**: FastAPI with async/await (future)
4. **Caching**: Redis for session data (future)
5. **Query Optimization**: Use of eager loading where needed

## Future Enhancements

1. **Farm Data Endpoints**: Water quality data sync
2. **Analytics**: Aggregated regional insights
3. **ML Model Distribution**: TFLite model downloads
4. **Rate Limiting**: Per-user API limits
5. **Websockets**: Real-time notifications
6. **Caching Layer**: Redis integration
7. **Message Queue**: Background job processing

## References

- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [SQLAlchemy Documentation](https://docs.sqlalchemy.org/)
- [Pydantic Documentation](https://docs.pydantic.dev/)
- [Firebase Admin SDK](https://firebase.google.com/docs/admin/setup)
- [PostGIS Documentation](https://postgis.net/)
