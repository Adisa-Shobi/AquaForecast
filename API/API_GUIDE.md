# AquaForecast API Guide

## Overview

Privacy-first REST API for aquaculture water quality data collection and ML model management. Built with FastAPI, PostgreSQL+PostGIS, and Firebase Authentication.

**Privacy Promise**: Only stores water quality parameters and location data. Predictions and pond configurations remain local on user devices.

## Quick Start

### Using Docker (Recommended)

```bash
cd API
cp .env.example .env
# Edit .env and add your remote DATABASE_URL
# Add your Firebase credentials file: firebase-credentials.json
docker-compose up -d
```

**Important**: The API uses a remote PostgreSQL database. Set `DATABASE_URL` in your `.env` file:
```
DATABASE_URL=postgresql://user:password@your-db-host.com:5432/aquaforecast
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

# Configure environment
cp .env.example .env
# Edit .env with your remote DATABASE_URL

# Add Firebase credentials
# Place firebase-credentials.json in API directory

# Ensure PostGIS is enabled on your remote database:
# Run: CREATE EXTENSION IF NOT EXISTS postgis;

# Run migrations
alembic upgrade head

# Start server
uvicorn app.main:app --reload
```

## API Endpoints

> **Full API documentation available at**: `http://localhost:8000/docs` (interactive OpenAPI/Swagger UI)

### Authentication

All protected endpoints require: `Authorization: Bearer <firebase_token>`

**POST /api/v1/auth/register** - Register user after Firebase authentication
**GET /api/v1/auth/me** - Get current user profile

### Farm Data

**POST /api/v1/farm-data/sync** - Bulk sync water quality readings (max 100 per request)
**GET /api/v1/farm-data** - Get historical readings with pagination
**GET /api/v1/farm-data/analytics** - Regional analytics and statistics
**DELETE /api/v1/farm-data/user** - Delete all user data (GDPR compliance)

### ML Model Management

#### Public (No Auth)
**GET /api/v1/models/latest** - Get latest model version
**GET /api/v1/models/deployed** - Get production model
**GET /api/v1/models/check-update** - Check for model updates

#### Authenticated
**GET /api/v1/models/list** - List all models with metrics
**GET /api/v1/models/{model_id}/metrics** - Detailed model metrics
**POST /api/v1/models/retrain** - Train new model from base model
**POST /api/v1/models/deploy** - Deploy model to production
**DELETE /api/v1/models/{model_id}/archive** - Archive model

### Health Check

**GET /health** - API status (no auth required)

## Architecture

```
API/
├── app/
│   ├── main.py           # FastAPI application
│   ├── core/             # Config, database, security
│   ├── models/           # SQLAlchemy ORM models
│   ├── schemas/          # Pydantic validation
│   ├── services/         # Business logic
│   └── api/v1/endpoints/ # Route handlers
├── alembic/              # Database migrations
└── requirements.txt
```

**Database**: PostgreSQL with PostGIS for spatial data
**Authentication**: Firebase ID tokens
**ML Storage**: Cloudinary CDN

## ML Model Management

The API supports continuous model improvement through automated retraining:

### Key Features
- **Version tracking** with parent-child relationships (semantic versioning)
- **Metrics**: R², RMSE, MAE tracked per model
- **Data integrity**: Training data tracked to prevent reuse
- **Cloud storage**: Models stored on Cloudinary CDN (TFLite + Keras)
- **One-click deployment**: Only one production model at a time

### Model Lifecycle
```
TRAINING → COMPLETED → DEPLOYED → ARCHIVED
```

### Training Workflow
1. **Select base model**: Use `/models/list` to find best model
2. **Initiate retraining**: `POST /models/retrain` with base_model_id and new version
3. **Monitor progress**: Training runs in background
4. **Review metrics**: Check R² score and errors
5. **Deploy**: `POST /models/deploy` if metrics improved

### Best Practices
- Retrain when 500+ new verified samples accumulated
- Only deploy if R² improvement ≥ 1%
- Use semantic versioning (e.g., 1.2.0)
- Document changes in notes field

**Note**: Model architecture and features detailed in training notebook

## Configuration

### Environment Variables

Create a `.env` file (see `.env.example`):

```bash
DATABASE_URL=postgresql://user:password@host:5432/aquaforecast
FIREBASE_CREDENTIALS_PATH=./firebase-credentials.json
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
CORS_ORIGINS=http://localhost:3000,https://your-frontend.vercel.app
```

Get Firebase credentials: Firebase Console → Project Settings → Service Accounts → Generate Key

## Data Validation

### Water Quality (Required)
- temperature: 0-50°C
- ph: 0-14
- dissolved_oxygen: 0-20 mg/L
- ammonia: 0-10 mg/L
- nitrate: 0-100 mg/L
- turbidity: 0-1000 NTU

### Fish Measurements (Optional)
- fish_weight: 0-100 kg
- fish_length: 0-500 cm
- verified: boolean (for training data quality)
- start_date: YYYY-MM-DD (pond cycle start)

## Database Migrations

```bash
alembic upgrade head              # Apply migrations
alembic revision --autogenerate   # Create new migration
alembic downgrade -1              # Rollback one version
```

## Deployment

```bash
# Docker (recommended)
docker-compose up -d

# View logs
docker-compose logs -f api

# Health check
curl http://localhost:8000/health
```

### Production Checklist
- Configure CORS for your frontend domains
- Use strong SECRET_KEY
- Enable HTTPS only
- Set up database backups
- Use environment secrets (not .env file in container)

## Technology Stack

- FastAPI 0.104+ (async web framework)
- PostgreSQL 14+ with PostGIS (spatial data)
- SQLAlchemy 2.0+ (ORM) + Alembic (migrations)
- Firebase Admin SDK (authentication)
- TensorFlow 2.15+ + scikit-learn (ML)
- Cloudinary (model storage & CDN)

## Support

**Interactive Docs**: http://localhost:8000/docs (OpenAPI/Swagger UI)
**API Limits**: Max 100 readings per sync, 1000 per query

---
**Version**: 1.0.0 | **Updated**: January 2025
