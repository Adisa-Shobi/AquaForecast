# Quick Start Guide

Get the Model Management Dashboard running in 5 minutes.

## Step 1: Install Dependencies

```bash
cd web
npm install
```

## Step 2: Start Backend API

In a separate terminal:

```bash
cd ../API
source venv/bin/activate
uvicorn app.main:app --reload
```

Backend should be running on `http://localhost:8000`

## Step 3: Start Web Dashboard

```bash
npm run dev
```

Dashboard opens at `http://localhost:3000`

## Step 4: Use the Dashboard

### View Models
- Dashboard shows all models automatically
- Click "View" to see detailed metrics and charts

### Retrain a Model
1. Click "Retrain Model" button
2. Select base model from dropdown
3. Enter new version (e.g., "1.1.0")
4. Click "Start Retraining"
5. Training runs in background - check dashboard for status

### Deploy a Model
1. Find a completed model in the list
2. Click "Deploy"
3. Review metrics comparison
4. Click "Deploy to Production"

## Troubleshooting

**"Failed to load models"**
- Ensure backend API is running
- Check: `curl http://localhost:8000/health`

**"401 Unauthorized"**
- Some endpoints require authentication
- For now, they're marked as protected but not enforced
- Full Firebase auth integration coming soon

**Port 3000 already in use**
```bash
# Edit vite.config.ts and change port:
server: {
  port: 3001,  // Use different port
}
```

## Next Steps

- See `README.md` for full documentation
- Check `API/API_GUIDE.md` for backend API details
- Explore the code in `src/pages/` for customization

Enjoy! 🎉
