# AquaForecast API - Quick Start Guide

Get the API running in under 5 minutes!

## Prerequisites

- Python 3.11+
- Docker Desktop (recommended) OR PostgreSQL 14+

## Option 1: Docker Compose (Recommended)

The fastest way to get started:

```bash
# 1. Navigate to API directory
cd AquaForecast/API

# 2. Copy environment file
cp .env.example .env

# 3. Add your Firebase credentials
# Download firebase-credentials.json from Firebase Console
# and place it in the API directory

# 4. Start everything with Docker
docker-compose up -d

# 5. View logs
docker-compose logs -f api

# 6. API is ready!
# Visit: http://localhost:8000/docs
```

That's it! The API is now running with a PostgreSQL database.

### Stop Services

```bash
docker-compose down
```

## Option 2: Local Development

For local development without Docker:

```bash
# 1. Create virtual environment
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# 2. Install dependencies
pip install -r requirements.txt

# 3. Set up PostgreSQL
createdb aquaforecast_dev
psql aquaforecast_dev -c "CREATE EXTENSION postgis;"

# 4. Configure environment
cp .env.example .env
# Edit .env with your database credentials

# 5. Add Firebase credentials
# Place firebase-credentials.json in API directory

# 6. Run migrations
alembic upgrade head

# 7. Start server
uvicorn app.main:app --reload

# API is ready at http://localhost:8000
```

## Verify Installation

```bash
# Check health
curl http://localhost:8000/health

# Expected response:
# {"status":"healthy","version":"1.0.0","environment":"development"}
```

## Test the API

### 1. Open Interactive Docs

Visit: http://localhost:8000/docs

### 2. Try the Endpoints

The interactive Swagger UI lets you test all endpoints directly in your browser.

## API Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/health` | Health check | No |
| POST | `/api/v1/auth/register` | Register user | Yes |
| GET | `/api/v1/auth/me` | Get user profile | Yes |

## Authentication

All protected endpoints require a Firebase ID token:

```bash
curl -X POST http://localhost:8000/api/v1/auth/register \
  -H "Authorization: Bearer YOUR_FIREBASE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firebase_uid": "your_firebase_uid",
    "email": "user@example.com"
  }'
```

## Getting Firebase Token

### From Android App

```kotlin
val user = FirebaseAuth.getInstance().currentUser
user?.getIdToken(true)?.addOnSuccessListener { result ->
    val token = result.token
    // Use this token for API requests
}
```

### For Testing (Python)

```python
import firebase_admin
from firebase_admin import credentials, auth

cred = credentials.Certificate("firebase-credentials.json")
firebase_admin.initialize_app(cred)

# Create custom token for testing
token = auth.create_custom_token("test_user_id")
```

## Project Structure

```
API/
├── app/
│   ├── main.py              # FastAPI app
│   ├── core/                # Config & database
│   ├── models/              # Database models
│   ├── schemas/             # Request/response schemas
│   ├── services/            # Business logic
│   └── api/v1/endpoints/    # API routes
├── tests/                   # Test suite
├── alembic/                 # Database migrations
└── requirements.txt         # Dependencies
```

## Common Commands

```bash
# Run tests
pytest

# Format code
black app tests

# Create migration
alembic revision --autogenerate -m "Description"

# Apply migrations
alembic upgrade head

# Rollback migration
alembic downgrade -1

# View logs (Docker)
docker-compose logs -f

# Restart API (Docker)
docker-compose restart api
```

## Troubleshooting

### Port 8000 already in use

```bash
# Kill process on port 8000
lsof -i :8000
kill -9 <PID>

# Or use different port
uvicorn app.main:app --reload --port 8001
```

### Database connection error

```bash
# Check PostgreSQL is running
psql -U postgres -c "SELECT 1"

# Check DATABASE_URL in .env
```

### Firebase error

- Ensure `firebase-credentials.json` exists
- Check `FIREBASE_CREDENTIALS_PATH` in `.env`
- Verify JSON file is valid

## Next Steps

1. Read [SETUP.md](SETUP.md) for detailed setup instructions
2. Review [ARCHITECTURE.md](ARCHITECTURE.md) to understand the codebase
3. Check [API_DOCUMENTATION.md](../API_DOCUMENTATION.md) for full API reference
4. Explore [README.md](README.md) for comprehensive documentation

## Support

- Check `/docs` for interactive API documentation
- Review test files in `tests/` for usage examples
- Read architecture docs for design decisions

## Privacy Notice

This API follows a privacy-first design:
- ✅ Only stores: Firebase UID, email, user ID
- ❌ Never stores: Profile info, predictions, pond configs
- 🔒 All sensitive data stays local on user devices

---

**Version**: 1.0.0
**Updated**: January 2025
