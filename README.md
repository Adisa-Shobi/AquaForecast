# Aquaculture Yield Prediction Model

## Description

An offline-capable machine learning system for predicting fish yield (weight and length) in West African aquaculture farms. This project leverages IoT sensor data from fish ponds to train a neural network that forecasts harvest outcomes for tilapia and catfish based on water quality parameters and farm conditions.

**Key Features:**
- Multi-output regression predicting both fish weight (kg) and length (cm)
- Engineered features including unionized ammonia, feeding efficiency, and 7-day rolling water quality metrics
- Neural network with LeakyReLU activation and strong L2 regularization (λ=0.06) optimized for small datasets
- Comprehensive preprocessing pipeline with biological limit capping and multivariate outlier detection
- Baseline model comparison (Random Forest, XGBoost, Neural Network)
- TensorFlow Lite model conversion for edge deployment on commodity smartphones
- Designed for resource-constrained environments with limited connectivity

**Target Performance:**
- R² > 0.75
- Model size < 5 MB
- Inference time < 100 ms

---

## Setup Instructions

### Installation

1. **Clone the repository:**
```bash
git clone <your-repo-url>
cd aquaculture-yield-prediction
```

2. **Install dependencies:**

```bash
pip install -r requirements.txt
```

3. Run the notebook

## Experiments

See experiments [here](https://docs.google.com/spreadsheets/d/1LL3BGybGi0Q1_xEeG3FdHI6GLnZUlm73LbU0vpVgYu4/edit?usp=sharing)

## Demo Video
 See [Demo](https://drive.google.com/file/d/1d-dE00gt8Q6ug2yrMfZKhYecPTGEeUJt/view?usp=sharing)
