# AquaForecast API v1

A privacy-first REST API for aquaculture water quality data collection and analytics.

## Overview

This API enables farmers to sync water quality parameters (temperature, pH, dissolved oxygen, ammonia, nitrate, turbidity) with user consent for aggregated analytics. All predictions, feeding schedules, and pond configurations remain local on user devices.

## Architecture

The API follows **Clean Architecture** principles with clear separation of concerns:

```
API/
├── app/
│   ├── __init__.py
│   ├── main.py                 # FastAPI application entry point
│   ├── core/                   # Core configuration and utilities
│   │   ├── __init__.py
│   │   ├── config.py          # Environment configuration
│   │   ├── security.py        # Security utilities
│   │   └── database.py        # Database connection
│   ├── models/                 # SQLAlchemy ORM models
│   │   ├── __init__.py
│   │   └── user.py
│   ├── schemas/                # Pydantic schemas (request/response)
│   │   ├── __init__.py
│   │   ├── user.py
│   │   └── common.py
│   ├── api/                    # API routes
│   │   ├── __init__.py
│   │   ├── deps.py            # Dependencies (auth, db session)
│   │   └── v1/
│   │       ├── __init__.py
│   │       ├── router.py      # Main v1 router
│   │       └── endpoints/
│   │           ├── __init__.py
│   │           └── auth.py
│   ├── services/               # Business logic layer
│   │   ├── __init__.py
│   │   └── user_service.py
│   └── middleware/             # Custom middleware
│       ├── __init__.py
│       └── error_handler.py
├── alembic/                    # Database migrations
│   ├── versions/
│   └── env.py
├── tests/                      # Test suite
│   ├── __init__.py
│   ├── conftest.py
│   └── test_auth.py
├── requirements.txt            # Python dependencies
├── .env.example               # Environment variables template
├── .gitignore
├── alembic.ini                # Alembic configuration
└── README.md
```

## Tech Stack

- **Framework**: FastAPI 0.104+
- **Database**: PostgreSQL 14+ with PostGIS extension
- **ORM**: SQLAlchemy 2.0+
- **Migration**: Alembic
- **Authentication**: Firebase Admin SDK
- **Validation**: Pydantic v2
- **ASGI Server**: Uvicorn

## Features (v1.0)

- ✅ Firebase Authentication integration
- ✅ User registration and profile management
- ✅ JWT token validation middleware
- ✅ RESTful API with OpenAPI/Swagger documentation
- ✅ Database migrations with Alembic
- ✅ Error handling and validation
- ✅ CORS configuration
- ✅ Request/Response logging

## Getting Started

### Prerequisites

- Python 3.11+
- PostgreSQL 14+ with PostGIS
- Firebase project with Admin SDK credentials

### Installation

1. **Clone the repository**
   ```bash
   cd API
   ```

2. **Create virtual environment**
   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

3. **Install dependencies**
   ```bash
   pip install -r requirements.txt
   ```

4. **Set up environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

5. **Download Firebase Admin SDK credentials**
   - Go to Firebase Console → Project Settings → Service Accounts
   - Generate new private key
   - Save as `firebase-credentials.json` in the API directory
   - **IMPORTANT**: Add to `.gitignore` (already included)

6. **Set up database**
   ```bash
   # Create database
   createdb aquaforecast_dev

   # Enable PostGIS extension
   psql aquaforecast_dev -c "CREATE EXTENSION postgis;"

   # Run migrations
   alembic upgrade head
   ```

7. **Run the development server**
   ```bash
   uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
   ```

8. **Access the API**
   - API: http://localhost:8000
   - Interactive docs: http://localhost:8000/docs
   - ReDoc: http://localhost:8000/redoc

## Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL connection string | `postgresql://user:pass@localhost/aquaforecast_dev` |
| `FIREBASE_CREDENTIALS_PATH` | Path to Firebase Admin SDK JSON | `./firebase-credentials.json` |
| `ENVIRONMENT` | Environment (development/staging/production) | `development` |
| `API_V1_PREFIX` | API v1 URL prefix | `/api/v1` |
| `CORS_ORIGINS` | Allowed CORS origins (comma-separated) | `http://localhost:3000,https://app.aquaforecast.com` |
| `LOG_LEVEL` | Logging level | `INFO` |

## API Endpoints

### Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/auth/register` | Register or verify user | Yes (Firebase) |
| GET | `/api/v1/auth/me` | Get current user info | Yes (Firebase) |

### Health Check

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/health` | API health status | No |

## Testing

```bash
# Run all tests
pytest

# Run with coverage
pytest --cov=app tests/

# Run specific test file
pytest tests/test_auth.py -v
```

## Database Migrations

```bash
# Create a new migration
alembic revision --autogenerate -m "Description of changes"

# Apply migrations
alembic upgrade head

# Rollback one migration
alembic downgrade -1

# View migration history
alembic history
```

## Project Structure Explained

### Core (`app/core/`)
Contains configuration, security utilities, and database setup. This is infrastructure code that the rest of the application depends on.

### Models (`app/models/`)
SQLAlchemy ORM models representing database tables. These define the data structure at the database level.

### Schemas (`app/schemas/`)
Pydantic models for request validation and response serialization. These define the API contract.

### API (`app/api/`)
API route handlers organized by version and feature. Each endpoint handles HTTP requests and delegates to services.

### Services (`app/services/`)
Business logic layer. Services contain the core application logic and interact with models.

### Middleware (`app/middleware/`)
Custom middleware for cross-cutting concerns like error handling and logging.

## Security

- All API endpoints use HTTPS in production
- Firebase ID tokens validated on every request
- SQL injection protection via SQLAlchemy ORM
- Input validation with Pydantic
- CORS properly configured
- Sensitive data never logged

## Deployment

### Using Docker (Recommended)

```bash
# Build image
docker build -t aquaforecast-api:v1 .

# Run container
docker run -d \
  --name aquaforecast-api \
  -p 8000:8000 \
  --env-file .env \
  aquaforecast-api:v1
```

### Manual Deployment

1. Set up PostgreSQL with PostGIS
2. Configure environment variables
3. Run migrations: `alembic upgrade head`
4. Start with production server: `gunicorn app.main:app -w 4 -k uvicorn.workers.UvicornWorker --bind 0.0.0.0:8000`

## Monitoring

- Health check endpoint: `/health`
- Metrics endpoint: `/metrics` (Prometheus-compatible)
- Structured JSON logging for easy parsing

## Contributing

1. Create a feature branch
2. Make your changes
3. Write/update tests
4. Ensure tests pass: `pytest`
5. Submit a pull request

## License

Proprietary - AquaForecast Project

## Support

For issues and questions, contact: dev@aquaforecast.com
