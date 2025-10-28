# AquaForecast API - Complete Guide

## Overview

Privacy-first REST API for aquaculture water quality data collection and analytics. Built with FastAPI, PostgreSQL+PostGIS, and Firebase Authentication.

**Privacy Promise**: Only stores water quality parameters and location data with user consent. All predictions, feeding schedules, and pond configurations remain local on user devices.

## Quick Start

### Using Docker (Recommended)

```bash
cd API
cp .env.example .env
# Add your Firebase credentials file: firebase-credentials.json
docker-compose up -d
```

API available at: http://localhost:8000
Docs at: http://localhost:8000/docs

### Manual Setup

```bash
# Create virtual environment
python3 -m venv venv
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Setup PostgreSQL
createdb aquaforecast_dev
psql aquaforecast_dev -c "CREATE EXTENSION postgis;"

# Configure environment
cp .env.example .env
# Edit .env with your settings

# Add Firebase credentials
# Place firebase-credentials.json in API directory

# Run migrations
alembic upgrade head

# Start server
uvicorn app.main:app --reload
```

## API Endpoints

### Authentication

**POST /api/v1/auth/register**
- Register or verify user after Firebase authentication
- Headers: `Authorization: Bearer <firebase_token>`
- Body: `{"firebase_uid": "...", "email": "..."}`
- Returns: User info (user_id, email, created_at)

**GET /api/v1/auth/me**
- Get current user profile
- Headers: `Authorization: Bearer <firebase_token>`
- Returns: User profile with reading count

### Farm Data

**POST /api/v1/farm-data/sync**
- Bulk sync water quality readings (max 100 per request)
- Headers: `Authorization: Bearer <firebase_token>`
- Body:
```json
{
  "device_id": "optional-device-id",
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
      "recorded_at": "2025-01-15T08:00:00Z"
    }
  ]
}
```
- Returns: Sync summary with synced_count, failed_count

**GET /api/v1/farm-data**
- Get user's historical readings
- Query params: `start_date`, `end_date`, `limit` (default 100), `offset`
- Returns: Paginated list of readings

**GET /api/v1/farm-data/analytics**
- Get aggregated analytics by region
- Query params: `region` (country code), `start_date`, `end_date`
- Returns: Regional averages and statistics

**DELETE /api/v1/farm-data/user**
- Delete all user's farm data (GDPR compliance)
- Returns: Count of deleted records

### ML Model Distribution

**GET /api/v1/models/latest**
- Get latest active ML model version
- No authentication required
- Returns: Model info (version, download URL, size, preprocessing config)

**GET /api/v1/models/check-update**
- Check if newer model version available
- Query params: `current_version` (required), `app_version` (optional)
- Returns: Update status, latest version info if available

### Health Check

**GET /health**
- Check API status (no auth required)
- Returns: `{"status": "healthy", "version": "1.0.0"}`

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    firebase_uid VARCHAR(128) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_sync_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);
```

### Model Versions Table
```sql
CREATE TABLE model_versions (
    id UUID PRIMARY KEY,
    version VARCHAR(50) UNIQUE NOT NULL,
    model_url TEXT NOT NULL,
    model_size_bytes BIGINT,
    preprocessing_config JSONB,
    is_active BOOLEAN DEFAULT TRUE,
    min_app_version VARCHAR(20),
    created_at TIMESTAMP NOT NULL
);
```

### Farm Data Table
```sql
CREATE TABLE farm_data (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    temperature DECIMAL(5, 2) NOT NULL,
    ph DECIMAL(4, 2) NOT NULL,
    dissolved_oxygen DECIMAL(5, 2) NOT NULL,
    ammonia DECIMAL(5, 2) NOT NULL,
    nitrate DECIMAL(5, 2) NOT NULL,
    turbidity DECIMAL(6, 2) NOT NULL,
    location GEOGRAPHY(POINT, 4326) NOT NULL,
    country_code VARCHAR(2),
    recorded_at TIMESTAMP NOT NULL,
    synced_at TIMESTAMP NOT NULL,
    device_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);
```

## Architecture

### Project Structure
```
API/
├── app/
│   ├── main.py                    # FastAPI application
│   ├── core/                      # Config, database, security
│   ├── models/                    # SQLAlchemy ORM models
│   ├── schemas/                   # Pydantic validation
│   ├── services/                  # Business logic
│   ├── api/v1/endpoints/          # Route handlers
│   └── middleware/                # Error handling
├── alembic/                       # Database migrations
├── tests/                         # Test suite
├── requirements.txt
├── Dockerfile
└── docker-compose.yml
```

### Clean Architecture Layers

1. **Core** - Infrastructure (config, database, security)
2. **Models** - Database tables (SQLAlchemy ORM)
3. **Schemas** - API validation (Pydantic)
4. **Services** - Business logic
5. **API** - HTTP handlers
6. **Middleware** - Cross-cutting concerns

### Request Flow
```
Client Request
    ↓
FastAPI Router
    ↓
Authentication Middleware (Firebase token validation)
    ↓
Pydantic Validation
    ↓
Endpoint Handler
    ↓
Service Layer (business logic)
    ↓
Database (SQLAlchemy)
    ↓
Response Serialization
    ↓
Client Response
```

## Configuration

### Environment Variables (.env)

```bash
# Environment
ENVIRONMENT=development

# Database
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/aquaforecast_dev

# Firebase
FIREBASE_CREDENTIALS_PATH=./firebase-credentials.json

# API
API_V1_PREFIX=/api/v1
PROJECT_NAME=AquaForecast API
VERSION=1.0.0

# CORS
CORS_ORIGINS=http://localhost:3000

# Logging
LOG_LEVEL=INFO

# Server
HOST=0.0.0.0
PORT=8000
```

### Getting Firebase Credentials

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Project Settings → Service Accounts
4. Generate new private key
5. Save as `firebase-credentials.json` in API directory

## Authentication

All protected endpoints require Firebase ID token in Authorization header:

```
Authorization: Bearer <firebase_id_token>
```

### Getting a Token (for testing)

From Android app:
```kotlin
FirebaseAuth.getInstance().currentUser?.getIdToken(true)
    ?.addOnSuccessListener { result ->
        val token = result.token
        // Use token in API requests
    }
```

Via Firebase REST API:
```bash
curl -X POST \
  'https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=YOUR_API_KEY' \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "user@example.com",
    "password": "password",
    "returnSecureToken": true
  }'
```

## Data Validation

### Water Quality Parameters
- **temperature**: 0-50°C (2 decimal places)
- **ph**: 0-14 (2 decimal places)
- **dissolved_oxygen**: 0-20 mg/L (2 decimal places)
- **ammonia**: 0-10 mg/L (2 decimal places)
- **nitrate**: 0-100 mg/L (2 decimal places)
- **turbidity**: 0-1000 NTU (2 decimal places)

### Location Data
- **latitude**: -90 to 90 (6 decimal places)
- **longitude**: -180 to 180 (6 decimal places)
- **country_code**: 2-letter ISO code (optional)

## Error Handling

### Response Format

**Success:**
```json
{
  "success": true,
  "data": { /* response data */ },
  "meta": { /* optional metadata */ }
}
```

**Error:**
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

### HTTP Status Codes
- **200 OK**: Success
- **201 Created**: Resource created
- **204 No Content**: Success, no response body
- **400 Bad Request**: Invalid input
- **401 Unauthorized**: Missing/invalid auth
- **403 Forbidden**: Not authorized
- **404 Not Found**: Resource not found
- **422 Unprocessable Entity**: Validation error
- **500 Internal Server Error**: Server error

## Testing

### Run Tests
```bash
# All tests
pytest

# With coverage
pytest --cov=app tests/

# Specific test file
pytest tests/test_auth.py -v
```

### Test Structure
- `tests/conftest.py` - Fixtures and test setup
- `tests/test_auth.py` - Authentication tests
- `tests/test_farm_data.py` - Farm data tests

## Database Migrations

### Create Migration
```bash
alembic revision --autogenerate -m "Description"
```

### Apply Migrations
```bash
alembic upgrade head
```

### Rollback
```bash
alembic downgrade -1
```

### View History
```bash
alembic history
```

## Deployment

### Using Docker

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

### Production Checklist
- [ ] Set strong SECRET_KEY
- [ ] Use production database
- [ ] Enable HTTPS only
- [ ] Configure CORS properly
- [ ] Set up monitoring/logging
- [ ] Configure rate limiting
- [ ] Set up database backups
- [ ] Use environment secrets (not .env file)

## Security

### Best Practices
- All endpoints use HTTPS in production
- Firebase tokens validated on every request
- SQL injection protected (SQLAlchemy ORM)
- Input validation with Pydantic
- CORS configured for allowed origins
- No sensitive data in logs
- Database connection pooling

### Privacy Compliance
**What we store:**
- ✅ Firebase UID, email, user ID
- ✅ Water quality parameters
- ✅ Location coordinates

**What we DON'T store:**
- ❌ User profile info (name, photo, phone)
- ❌ Farm/pond configurations
- ❌ Predictions
- ❌ Feeding schedules

## Monitoring

### Health Check
```bash
curl http://localhost:8000/health
```

### Logs
Structured JSON logging for easy parsing. View logs:
```bash
# Docker
docker-compose logs -f api

# Local
tail -f logs/api.log
```

## Troubleshooting

### Database Connection Error
```bash
# Check PostgreSQL is running
psql -U postgres -c "SELECT 1"

# Verify DATABASE_URL in .env
```

### Firebase Authentication Error
```bash
# Verify firebase-credentials.json exists
ls -la firebase-credentials.json

# Check FIREBASE_CREDENTIALS_PATH in .env
```

### Port Already in Use
```bash
# Find process
lsof -i :8000

# Kill process
kill -9 <PID>

# Or use different port
uvicorn app.main:app --port 8001
```

### PostGIS Extension Missing
```bash
psql aquaforecast_dev -c "CREATE EXTENSION postgis;"
```

## Development

### Code Style
```bash
# Format with black
black app tests

# Check with flake8
flake8 app tests

# Type checking
mypy app
```

### Adding New Endpoints

1. Create schema in `app/schemas/`
2. Add service logic in `app/services/`
3. Create endpoint in `app/api/v1/endpoints/`
4. Register in `app/api/v1/router.py`
5. Write tests in `tests/`

### Database Changes

1. Modify model in `app/models/`
2. Generate migration: `alembic revision --autogenerate -m "..."`
3. Review migration file
4. Apply: `alembic upgrade head`

## Technology Stack

- **FastAPI 0.104+** - Modern async web framework
- **PostgreSQL 14+** - Relational database
- **PostGIS 3.3+** - Spatial database extension
- **SQLAlchemy 2.0+** - ORM
- **Alembic 1.12+** - Database migrations
- **Pydantic v2** - Data validation
- **Firebase Admin SDK** - Authentication
- **Pytest** - Testing framework
- **Uvicorn** - ASGI server

## API Limits

- Max readings per sync: 100
- Max farm data query: 1000 records
- Token expiry: 1 hour (Firebase default)

## Support

- Interactive API docs: http://localhost:8000/docs
- OpenAPI spec: http://localhost:8000/openapi.json
- GitHub: [Project Repository]
- Email: dev@aquaforecast.com

---

**Version**: 1.0.0
**Last Updated**: January 2025
**License**: Proprietary
