# AquaForecast Model Management Dashboard

Web-based admin panel for managing ML models, viewing metrics, and deploying to production.

## Features

- 📊 **Dashboard** - View all models with status, metrics, and deployment info
- 📈 **Detailed Metrics** - Interactive charts showing training history and performance
- 🔄 **Model Retraining** - Retrain models from any completed base model
- 🚀 **Deployment Management** - One-click deployment with comparison
- 🎯 **Real-time Status** - Monitor training progress and model status
- 📋 **Model Lineage** - Track parent-child relationships

## Tech Stack

- **React 18** - UI framework
- **TypeScript** - Type safety
- **Vite** - Build tool
- **Tailwind CSS** - Styling
- **Recharts** - Data visualization
- **React Router** - Navigation
- **Axios** - API client

## Prerequisites

- Node.js 18+ and npm
- Backend API running on `http://localhost:8000`
- Firebase authentication token (for authenticated requests)

## Installation

```bash
# Navigate to web directory
cd web

# Install dependencies
npm install
```

## Development

```bash
# Start development server
npm run dev

# Open browser to http://localhost:3000
```

The dev server proxies `/api` requests to `http://localhost:8000`.

## Build for Production

```bash
# Build optimized production bundle
npm run build

# Preview production build
npm run preview
```

## Project Structure

```
web/
├── src/
│   ├── api/
│   │   └── client.ts          # API client with model endpoints
│   ├── pages/
│   │   ├── Dashboard.tsx      # Main model list view
│   │   ├── ModelDetails.tsx   # Detailed metrics & charts
│   │   ├── RetrainModel.tsx   # Retraining form
│   │   └── DeployModel.tsx    # Deployment interface
│   ├── types/
│   │   └── model.ts           # TypeScript interfaces
│   ├── App.tsx                # Main app component
│   ├── main.tsx               # Entry point
│   └── index.css              # Global styles
├── package.json
├── vite.config.ts
├── tailwind.config.js
└── tsconfig.json
```

## Usage

### Dashboard

The dashboard shows all models with:
- Version number and status
- Overall R² score
- Training data count
- Model size (TFLite)
- Creation date
- Quick actions (View, Deploy)

**Filters:**
- Toggle "Include archived models" to show/hide archived models

### View Model Details

Click "View" on any model to see:
- Overall metrics (R², RMSE, MAE)
- Per-target metrics (fish_weight, fish_length)
- Training history charts (loss and MAE over epochs)
- Training duration and data count

### Retrain a Model

1. Click "Retrain Model" button
2. Select a **base model** (must be completed)
3. Enter **new version** (semantic versioning recommended)
4. Adjust training parameters:
   - Epochs (default: 100)
   - Batch size (default: 32)
   - Learning rate (default: 0.000006)
5. Add optional notes
6. Click "Start Retraining"

**Important:**
- Only unused farm data will be used (no data reuse)
- Minimum 100 samples required
- Training runs in background
- Check dashboard for status updates

### Deploy a Model

1. Click "Deploy" on a completed model (or navigate from dashboard)
2. Review comparison with current deployed model
3. Add deployment notes (optional)
4. Click "Deploy to Production"

**Effects:**
- Previous deployed model is automatically undeployed
- Mobile apps will receive the new model on next update check
- Deployment notes are saved to model history

## API Integration

The app connects to the backend API at `/api/v1`. All endpoints are defined in `src/api/client.ts`:

- `GET /models/list` - List all models
- `GET /models/deployed` - Get deployed model
- `GET /models/{id}/metrics` - Get detailed metrics
- `POST /models/retrain` - Initiate retraining
- `POST /models/deploy` - Deploy model
- `DELETE /models/{id}/archive` - Archive model

### Authentication

To add Firebase authentication:

1. Set token in API client:
```typescript
import { setAuthToken } from './api/client';

// After Firebase login
const token = await user.getIdToken();
setAuthToken(token);
```

2. Token is automatically included in all API requests via `Authorization: Bearer <token>` header

## Environment Variables

Create `.env` file for custom configuration:

```bash
# API base URL (if not using proxy)
VITE_API_URL=http://localhost:8000/api/v1

# Firebase config (if implementing auth)
VITE_FIREBASE_API_KEY=your_api_key
VITE_FIREBASE_AUTH_DOMAIN=your_auth_domain
VITE_FIREBASE_PROJECT_ID=your_project_id
```

## Customization

### Styling

The app uses Tailwind CSS. Customize theme in `tailwind.config.js`:

```js
module.exports = {
  theme: {
    extend: {
      colors: {
        primary: '#3B82F6',  // Customize primary color
      },
    },
  },
}
```

### API Endpoint

Change the API proxy in `vite.config.ts`:

```typescript
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://your-api-url:8000',  // Change this
        changeOrigin: true,
      },
    },
  },
})
```

## Features Breakdown

### Dashboard Page
- Model list table with sorting
- Status badges (training, completed, deployed, etc.)
- Quick stats cards (total, deployed, completed, training)
- Filter by archived status
- Navigate to details, deployment, or retraining

### Model Details Page
- Comprehensive metrics display
- Interactive training history charts (Recharts)
- Loss and MAE visualization over epochs
- Per-target (weight/length) breakdown
- Training metadata (duration, data count)

### Retrain Page
- Base model selection dropdown
- Version input with validation
- Configurable training parameters
- Real-time form validation
- Info box with retraining details

### Deploy Page
- Side-by-side model comparison
- Metrics improvement indicator
- Deployment notes textarea
- Confirmation dialog
- Automatic undeploy of previous model

## Troubleshooting

### API Connection Errors

**Problem:** "Failed to load models"
- **Solution:** Ensure backend API is running on `http://localhost:8000`
- **Check:** `curl http://localhost:8000/health`

### CORS Errors

**Problem:** CORS policy blocking requests
- **Solution:** Add CORS origins in backend `.env`:
  ```bash
  CORS_ORIGINS=http://localhost:3000
  ```

### 401 Unauthorized

**Problem:** API returns 401 for authenticated endpoints
- **Solution:** Set authentication token using `setAuthToken(token)`
- **Check:** Token is valid Firebase ID token

### Chart Not Rendering

**Problem:** Training history chart is blank
- **Solution:** Ensure model has `training_history` data
- **Check:** Some models may not have training history if training failed

## Production Deployment

### Build

```bash
npm run build
# Output in dist/ directory
```

### Deploy to Nginx

```nginx
server {
    listen 80;
    server_name admin.aquaforecast.com;
    root /var/www/aquaforecast-web/dist;
    index index.html;

    # SPA routing
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API proxy
    location /api/ {
        proxy_pass http://localhost:8000/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### Deploy to Vercel

```bash
# Install Vercel CLI
npm i -g vercel

# Deploy
vercel

# Configure environment variables in Vercel dashboard
# Add rewrites in vercel.json for API proxy
```

## Contributing

1. Create feature branch
2. Make changes
3. Test locally
4. Submit pull request

## License

Proprietary - AquaForecast

---

**Questions?** See main API documentation at `/API/API_GUIDE.md`
