# Aquaculture Yield Prediction Model

[Demo](https://drive.google.com/file/d/1JClGtGSsxioLgnkSlJUuQkZGVWlpkANw/view?usp=sharing)

## Description

An offline-capable machine learning system for predicting fish yield (weight and length) in West African aquaculture farms. This project leverages IoT sensor data from fish ponds to train a neural network that forecasts harvest outcomes for tilapia and catfish based on water quality parameters and farm conditions.

## Getting Started

### Project Structure

- **[API/](./API/)** - FastAPI backend ([Documentation](./API/API_GUIDE.md))
- **[web/](./web/)** - React dashboard ([Documentation](./web/README.md))

### Quick Start

#### Backend API

```bash
cd API
docker-compose up -d  # Runs at http://localhost:8000
```

See [API_GUIDE.md](./API/API_GUIDE.md) for configuration and endpoints.

#### Web Dashboard

```bash
cd web
npm install && npm run dev  # Runs at http://localhost:3000
```

See [web/README.md](./web/README.md) for environment setup.

#### Android App

```bash
cd client
./gradlew assembleDebug  # APK in app/build/outputs/apk/debug/
```

See [client/CLAUDE.md](./client/CLAUDE.md) for build instructions and architecture.

**Download APK:** [Google Drive](https://drive.google.com/file/d/1Nb0TSycHMcoM8qfr65ZGiUy0UDBWKt8X/view?usp=sharing)

## Testing Results

### Functionality Testing Strategies

| Test Type | Screenshot 1 | Screenshot 2 |
|-----------|-------------|-------------|
| Memory Leak Test | ![Memory leaks](https://i.imgur.com/dUrjDix.gif) | ![Memory leaks](https://i.imgur.com/0843HlO.png) |
| Unit Tests | ![Unit tests](https://i.imgur.com/4syUEer.png) | |

### Functionality with Different Data Values

| Data Range | Water Quality | Dashboard | History |
|------------|--------------|-----------|---------|
| Normal range | ![Quality normal](https://i.imgur.com/MJsPJV8.png) | ![Normal Dash](https://i.imgur.com/rrR46cp.png) | ![Normal History](https://i.imgur.com/RJgn7e0.png) |
| Moderately out of range | ![Quality warning](https://i.imgur.com/y8PRvjo.png) | ![Warning Dash](https://i.imgur.com/qLkDjVc.png) | ![Warning History](https://i.imgur.com/0TS5ef4.png) |
| Highly out of range | ![Quality error](https://i.imgur.com/mCPOpej.png) | ![Error Dash](https://i.imgur.com/bq9fVHh.png) | ![Error history](https://i.imgur.com/u1cW4jn.png) |
| Denied Location | ![Denied Location](https://i.imgur.com/JC5YVi0.png) | | |
| Offline Mode | ![Offline mode](https://i.imgur.com/h6NFRk2.png) | | |

### Performance on Different Hardware

| Device | Performance |
|--------|------------|
| Pixel 7 Pro (API 33) | ![Pixel 7 Pro](https://i.imgur.com/sciuTWI.gif) |
| Pixel Tablet (API 33) | ![Pixel Tablet](https://i.imgur.com/Tai2Kmr.gif) |
| Small Android (API 24) | ![Small Android](https://i.imgur.com/hncApJZ.gif) |

## Analysis

### Objectives Achievement

**Offline Framework**

Achieved - App runs entirely offline using TensorFlow Lite for all core features.

**Prediction Accuracy**

Achieved - Neural network achieved R² of 0.9761 (RMSE: 3.1374, MAE: 2.0011), exceeding target of 0.75.

**Performance**

Achieved - Model meets size and speed targets. Compatible with Android API 24+.

**Usability**

Achieved - 5-screen interface with color-coded water quality indicators tested across device specifications.

### Challenges and Solutions

**Limited Data**

10-pond Nigerian dataset. Mitigated with L2 regularization and engineered features.

**Data Quality**

5-10% loss from invalid readings. Addressed with outlier detection and imputation.

**Device Constraints**

Optimized through model quantization and efficient data handling.

## Discussion

### Impact

**Accessibility**

Eliminates internet requirement for precision aquaculture on affordable smartphones.

**Value**

Enables feed optimization, harvest timing, and early warning for water quality issues.

### Limitations

- Training data from Nigerian ponds only (June-October 2021)
- Supports African Catfish and Tilapia species only
- Requires daily manual water quality data entry
- May need recalibration for different regions/conditions

### Key Finding

Ammonia showed 3-4x higher feature importance than other parameters, confirming its critical role in fish growth.

## Recommendations

### For Users

- Validate predictions against actual harvests for first 2-3 months
- Prioritize ammonia testing as most critical parameter
- Follow color-coded alerts: Green (continue), Yellow (monitor), Red (immediate action)

### Future Work

- Expand training data to other West African regions and full seasonal cycles
- Add Bluetooth sensor integration to reduce manual entry
- Implement transfer learning for new regions with limited data
- Explore LSTM models for temporal pattern recognition

## Links

- **GitHub**: [https://github.com/Adisa-Shobi/AquaForecast](https://github.com/Adisa-Shobi/AquaForecast)
- **APK Download**: [Google Drive](https://drive.google.com/file/d/1Nb0TSycHMcoM8qfr65ZGiUy0UDBWKt8X/view?usp=sharing)
