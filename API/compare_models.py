"""
Compare baseline model vs newly trained model performance.

This script evaluates both models on the same validation dataset and
calculates percentage improvement in metrics.
"""

import os
import sys
import argparse
import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.metrics import mean_squared_error, mean_absolute_error, r2_score
from sklearn.preprocessing import RobustScaler
import joblib

# Add app to path
sys.path.insert(0, os.path.dirname(__file__))

from app.core.database import SessionLocal
from app.services.model_training_service import ModelTrainingService


def load_model(model_path):
    """Load a Keras model."""
    if not os.path.exists(model_path):
        raise FileNotFoundError(f"Model not found: {model_path}")

    print(f"Loading model from: {model_path}")
    model = tf.keras.models.load_model(model_path)
    return model


def load_scaler(scaler_path):
    """Load a scaler."""
    if not os.path.exists(scaler_path):
        raise FileNotFoundError(f"Scaler not found: {scaler_path}")

    print(f"Loading scaler from: {scaler_path}")
    scaler = joblib.load(scaler_path)
    return scaler


def prepare_validation_data(limit=None, use_all_data=False):
    """
    Prepare validation data from database.

    Args:
        limit: Optional limit on number of samples
        use_all_data: If True, use all available farm data instead of just unused

    Returns:
        Tuple of (X_scaled, y, scaler, feature_names)
    """
    print("\n" + "="*80)
    print("PREPARING VALIDATION DATA")
    print("="*80)

    db = SessionLocal()
    try:
        if use_all_data:
            # Get all farm data IDs
            from app.models.farm_data import FarmData
            farm_data_ids = [str(fd.id) for fd in db.query(FarmData.id).all()]
            print(f"Using all available farm data (use_all_data=True)")
        else:
            # Get unused farm data for validation
            farm_data_ids = ModelTrainingService.get_unused_farm_data(db)
            print(f"Using only unused farm data")

        if limit:
            farm_data_ids = farm_data_ids[:limit]

        print(f"Found {len(farm_data_ids)} farm data records")

        if len(farm_data_ids) == 0:
            raise ValueError("No farm data available for validation")

        # Fetch and preprocess data
        df = ModelTrainingService.fetch_training_data(db, farm_data_ids)
        print(f"Fetched {len(df)} records after initial processing")

        # Preprocess
        config = ModelTrainingService.DEFAULT_CONFIG
        df_clean = ModelTrainingService.preprocess_training_data(
            df,
            config['feature_cols'],
            config['target_cols']
        )
        print(f"After preprocessing: {len(df_clean)} records")

        # Extract features and targets
        X = df_clean[config['feature_cols']].values
        y = df_clean[config['target_cols']].values

        print(f"\nValidation data shape:")
        print(f"  Features (X): {X.shape}")
        print(f"  Targets (y):  {y.shape}")
        print(f"\nTarget statistics:")
        print(f"  Weight: mean={y[:, 0].mean():.1f}g, std={y[:, 0].std():.1f}g, range=[{y[:, 0].min():.1f}, {y[:, 0].max():.1f}]")
        print(f"  Length: mean={y[:, 1].mean():.1f}cm, std={y[:, 1].std():.1f}cm, range=[{y[:, 1].min():.1f}, {y[:, 1].max():.1f}]")

        return X, y, config['feature_cols']

    finally:
        db.close()


def evaluate_model(model, X_scaled, y_true, model_name):
    """
    Evaluate a model and return metrics.

    Args:
        model: Keras model
        X_scaled: Scaled input features
        y_true: True target values
        model_name: Name for display

    Returns:
        Dictionary with metrics
    """
    print(f"\n{'-'*80}")
    print(f"Evaluating: {model_name}")
    print(f"{'-'*80}")

    # Make predictions
    y_pred = model.predict(X_scaled, verbose=0)

    # Overall metrics
    r2 = r2_score(y_true, y_pred)
    rmse = np.sqrt(mean_squared_error(y_true, y_pred))
    mae = mean_absolute_error(y_true, y_pred)

    # Per-target metrics
    r2_weight = r2_score(y_true[:, 0], y_pred[:, 0])
    rmse_weight = np.sqrt(mean_squared_error(y_true[:, 0], y_pred[:, 0]))
    mae_weight = mean_absolute_error(y_true[:, 0], y_pred[:, 0])

    r2_length = r2_score(y_true[:, 1], y_pred[:, 1])
    rmse_length = np.sqrt(mean_squared_error(y_true[:, 1], y_pred[:, 1]))
    mae_length = mean_absolute_error(y_true[:, 1], y_pred[:, 1])

    # Prediction statistics
    pred_mean = y_pred.mean(axis=0)
    pred_std = y_pred.std(axis=0)
    true_mean = y_true.mean(axis=0)
    true_std = y_true.std(axis=0)

    print(f"\nOverall Metrics:")
    print(f"  R²:   {r2:.4f}")
    print(f"  RMSE: {rmse:.2f}")
    print(f"  MAE:  {mae:.2f}")

    print(f"\nWeight Metrics:")
    print(f"  R²:   {r2_weight:.4f}")
    print(f"  RMSE: {rmse_weight:.2f}g")
    print(f"  MAE:  {mae_weight:.2f}g")

    print(f"\nLength Metrics:")
    print(f"  R²:   {r2_length:.4f}")
    print(f"  RMSE: {rmse_length:.2f}cm")
    print(f"  MAE:  {mae_length:.2f}cm")

    return {
        'name': model_name,
        'overall': {
            'r2': r2,
            'rmse': rmse,
            'mae': mae
        },
        'weight': {
            'r2': r2_weight,
            'rmse': rmse_weight,
            'mae': mae_weight
        },
        'length': {
            'r2': r2_length,
            'rmse': rmse_length,
            'mae': mae_length
        },
        'predictions': {
            'mean': pred_mean,
            'std': pred_std
        },
        'actuals': {
            'mean': true_mean,
            'std': true_std
        }
    }


def calculate_improvement(baseline_metrics, new_metrics):
    """
    Calculate percentage improvement from baseline to new model.

    Positive percentage = improvement (new model is better)
    Negative percentage = degradation (new model is worse)

    Args:
        baseline_metrics: Metrics dict from baseline model
        new_metrics: Metrics dict from new model

    Returns:
        Dictionary with percentage improvements
    """
    print("\n" + "="*80)
    print("PERCENTAGE IMPROVEMENT ANALYSIS")
    print("="*80)
    print("\nFormula: ((New - Baseline) / Baseline) * 100")
    print("For error metrics (RMSE, MAE): Negative % = improvement")
    print("For R²: Positive % = improvement")

    improvements = {}

    # Overall improvements
    print(f"\n{'Metric':<20} {'Baseline':<12} {'New Model':<12} {'Change %':<12} {'Status'}")
    print("-"*80)

    for category in ['overall', 'weight', 'length']:
        improvements[category] = {}

        if category == 'overall':
            print(f"\n{category.upper()} METRICS:")
        elif category == 'weight':
            print(f"\n{category.upper()} (Fish Weight):")
        else:
            print(f"\n{category.upper()} (Fish Length):")

        for metric in ['r2', 'rmse', 'mae']:
            baseline_val = baseline_metrics[category][metric]
            new_val = new_metrics[category][metric]

            if baseline_val == 0:
                pct_change = 0
            else:
                pct_change = ((new_val - baseline_val) / abs(baseline_val)) * 100

            # For R², higher is better
            # For RMSE/MAE, lower is better
            if metric == 'r2':
                status = "✓ Better" if pct_change > 0 else "✗ Worse"
                arrow = "↑" if pct_change > 0 else "↓"
            else:
                status = "✓ Better" if pct_change < 0 else "✗ Worse"
                arrow = "↓" if pct_change < 0 else "↑"

            improvements[category][metric] = pct_change

            metric_display = metric.upper()
            print(f"  {metric_display:<18} {baseline_val:>10.4f}  {new_val:>10.4f}  {arrow} {pct_change:>8.2f}%  {status}")

    return improvements


def print_summary(baseline_metrics, new_metrics, improvements):
    """Print a summary of the comparison."""
    print("\n" + "="*80)
    print("SUMMARY")
    print("="*80)

    # Count improvements vs degradations
    better_count = 0
    worse_count = 0

    for category in ['overall', 'weight', 'length']:
        for metric, pct in improvements[category].items():
            if metric == 'r2':
                if pct > 0:
                    better_count += 1
                else:
                    worse_count += 1
            else:  # RMSE, MAE
                if pct < 0:
                    better_count += 1
                else:
                    worse_count += 1

    total_metrics = better_count + worse_count

    print(f"\nMetrics improved: {better_count}/{total_metrics} ({better_count/total_metrics*100:.1f}%)")
    print(f"Metrics degraded: {worse_count}/{total_metrics} ({worse_count/total_metrics*100:.1f}%)")

    # Key improvements
    overall_r2_improvement = improvements['overall']['r2']
    overall_mae_improvement = improvements['overall']['mae']

    print(f"\nKey Improvements:")
    print(f"  Overall R² change:  {overall_r2_improvement:+.2f}%")
    print(f"  Overall MAE change: {overall_mae_improvement:+.2f}%")

    if overall_r2_improvement > 0 and overall_mae_improvement < 0:
        print(f"\n✓ NEW MODEL IS BETTER - Both R² and MAE improved!")
    elif overall_r2_improvement < 0 and overall_mae_improvement > 0:
        print(f"\n✗ BASELINE MODEL IS BETTER - Both R² and MAE degraded")
    else:
        print(f"\n~ MIXED RESULTS - Some metrics improved, others degraded")


def main():
    parser = argparse.ArgumentParser(description="Compare baseline vs new model performance")
    parser.add_argument(
        '--baseline',
        type=str,
        default='app/ml_models/default/baseline_model.keras',
        help='Path to baseline model'
    )
    parser.add_argument(
        '--new-model',
        type=str,
        required=True,
        help='Path to new model to compare'
    )
    parser.add_argument(
        '--baseline-scaler',
        type=str,
        default='app/ml_models/default/default_scaler.pkl',
        help='Path to baseline scaler'
    )
    parser.add_argument(
        '--new-scaler',
        type=str,
        required=True,
        help='Path to new model scaler'
    )
    parser.add_argument(
        '--limit',
        type=int,
        default=None,
        help='Limit number of validation samples (for testing)'
    )
    parser.add_argument(
        '--use-all-data',
        action='store_true',
        help='Use all farm data instead of just unused data (useful when no unused data available)'
    )

    args = parser.parse_args()

    print("="*80)
    print("MODEL COMPARISON TOOL")
    print("="*80)
    print(f"\nBaseline Model: {args.baseline}")
    print(f"New Model:      {args.new_model}")
    print(f"Baseline Scaler: {args.baseline_scaler}")
    print(f"New Scaler:      {args.new_scaler}")

    # Load models
    baseline_model = load_model(args.baseline)
    new_model = load_model(args.new_model)

    # Load scalers
    baseline_scaler = load_scaler(args.baseline_scaler)
    new_scaler = load_scaler(args.new_scaler)

    # Prepare validation data (unscaled)
    X_val, y_val, feature_names = prepare_validation_data(limit=args.limit, use_all_data=args.use_all_data)

    # Scale data for each model (they may have different scalers)
    print("\nScaling data for baseline model...")
    X_val_baseline = baseline_scaler.transform(X_val)

    print("Scaling data for new model...")
    X_val_new = new_scaler.transform(X_val)

    # Evaluate both models
    baseline_metrics = evaluate_model(baseline_model, X_val_baseline, y_val, "BASELINE MODEL")
    new_metrics = evaluate_model(new_model, X_val_new, y_val, "NEW MODEL")

    # Calculate improvements
    improvements = calculate_improvement(baseline_metrics, new_metrics)

    # Print summary
    print_summary(baseline_metrics, new_metrics, improvements)

    print("\n" + "="*80)
    print("COMPARISON COMPLETE")
    print("="*80)


if __name__ == "__main__":
    main()
