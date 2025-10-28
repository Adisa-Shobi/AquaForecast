# Assets Folder

## Required Files

### 1. TensorFlow Lite Model

Place your trained TFLite model file in this directory:

```
aqua_forecast_model.tflite
```

### 2. Preprocessing Configuration

The preprocessing configuration is stored in:

```
preprocessing_config.json
```

This file contains:
- **scaler**: RobustScaler parameters (center and scale values for 16 features)
- **imputation_medians**: Fallback values for missing data
- **biological_limits**: Min/max caps for sensor readings
- **constants**: Optimal DO and initial day offset
- **target_columns**: Model output column names

## Model Requirements

### Input: 15 Features (Normalized using RobustScaler)

**Base features (7):**
1. temperature(c)
2. ph
3. dissolved_oxygen(g/ml)
4. ammonia(g/ml)
5. nitrate(g/ml)
6. turbidity(ntu)
7. population

**Engineered features (8):**
8. days_in_farm - Days since pond creation
9. day_of_year - Seasonal feature (1-366)
10. hour - Hour of day (0-23)
11. sin_hour - Cyclic encoding of hour
12. cos_hour - Cyclic encoding of hour
13. temp_do_interaction - temperature × dissolved_oxygen
14. avg_do_7d - 7-day rolling average of DO
16. avg_wqi_7d - Water quality deviation: |avg_do_7d - optimal_do| / optimal_do

### Output: 2 Predictions

- [0] Predicted fish weight (grams)
- [1] Predicted fish length (cm)

## Preprocessing Pipeline (7 Steps)

The `FeaturePreprocessor` class implements this exact pipeline:

1. **Get Current Farm Data** - Retrieve 7 base features from database
2. **Apply Biological Caps** - Clip values to realistic ranges using biological_limits
3. **Engineer Features** - Calculate 9 additional features
4. **Create Ordered Array** - Combine all 16 features in exact order from config
5. **Impute Missing Values** - Replace NaN with imputation_medians
6. **Ensure Non-Negatives** - Apply max(0, value) to specific features
7. **Scale Features** - Apply RobustScaler: (value - center) / scale

## Updating Configuration

To update preprocessing parameters from your trained model:

1. **Extract scaler parameters** from your Python training pipeline:
   ```python
   import json

   config = {
       "scaler": {
           "center": scaler.center_.tolist(),
           "scale": scaler.scale_.tolist(),
           "feature_names": feature_names
       },
       # ... other config values
   }

   with open('preprocessing_config.json', 'w') as f:
       json.dump(config, f, indent=2)
   ```

2. **Replace** `preprocessing_config.json` in this directory

3. **Rebuild** the app to include the updated configuration

### Testing the Model

Once you place the model file:
1. Build and run the app
2. Configure a pond in Settings
3. Add farm data entries
4. Navigate to Predictions screen to see forecasts

### Model Training

Ensure your model is trained with:
- Input shape: `(batch_size, 15)`
- Output shape: `(batch_size, 2)`
- Compatible with TensorFlow Lite format

### Troubleshooting

If you encounter errors:
- Verify the model file name is exactly `aqua_forecast_model.tflite`
- Check that the model input/output dimensions match expectations
- Update scaler parameters in `FeaturePreprocessor.kt`
- Review logs for detailed error messages
