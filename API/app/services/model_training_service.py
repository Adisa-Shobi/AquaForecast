"""Model training and retraining service."""

import os
import time
import tempfile
import json
import logging
from typing import Any, Dict, List, Optional, Tuple
from datetime import datetime
from pathlib import Path
import uuid

import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers, models, callbacks
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.regularizers import l2
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import RobustScaler
from sklearn.ensemble import IsolationForest
from sklearn.metrics import mean_squared_error, mean_absolute_error, r2_score
from sqlalchemy.orm import Session
import joblib

from app.models.model_version import ModelVersion, TrainingSession, ModelStatus
from app.models.farm_data import FarmData
from app.core.cloudinary_storage import cloudinary_storage

logger = logging.getLogger(__name__)


class ModelTrainingService:
    """Service for training and retraining ML models."""

    # Default training configuration (matches aqua_forecast.py)
    DEFAULT_CONFIG = {
        'model_params': {
            'nn': {
                'layer_1_units': 64,
                'layer_2_units': 32,
                'layer_3_units': 16,
                'dropout_rate_1': 0.08,
                'dropout_rate_2': 0.08,
                'learning_rate': 0.000006,
                'l2_lambda': 0.06
            },
            'training': {
                'epochs': 100,
                'batch_size': 32,
                'early_stop_patience': 15,  # Increased for transfer learning with new data distribution
                'reduce_lr_patience': 5,    # Increased to give model more time to adapt
                'reduce_lr_factor': 0.5,    # More aggressive LR reduction
                'min_lr': 1e-7
            }
        },
        'preprocessing': {
            'test_size': 0.15,
            'val_size': 0.15,
            'random_state': 42,
            'rolling_window': 7,
            'optimal_do': 6.0,
        },
        'feature_cols': [
            # Base features (6)
            'temperature', 'ph', 'dissolved_oxygen',
            'ammonia', 'nitrate', 'turbidity',
            # Engineered features (8)
            'days_in_farm', 'day_of_year', 'hour',
            'sin_hour', 'cos_hour', 'temp_do_interaction',
            'avg_do_7d', 'avg_wqi_7d'
        ],
        'target_cols': ['fish_weight', 'fish_length']
    }

    @staticmethod
    def get_unused_farm_data(db: Session) -> List[uuid.UUID]:
        """
        Get farm data IDs that haven't been used for training.

        Returns:
            List of farm data UUIDs
        """
        # Get all farm data IDs
        all_data = db.query(FarmData.id).filter(
            FarmData.fish_weight.isnot(None),
            FarmData.fish_length.isnot(None),
            FarmData.verified == True
        ).all()
        all_data_ids = {row.id for row in all_data}

        # Get all used data IDs from training sessions
        used_sessions = db.query(TrainingSession.farm_data_ids).all()
        used_data_ids = set()
        for session in used_sessions:
            if session.farm_data_ids:
                used_data_ids.update(session.farm_data_ids)

        # Return unused IDs
        unused_ids = list(all_data_ids - used_data_ids)
        return unused_ids

    @staticmethod
    def fetch_training_data(db: Session, farm_data_ids: List[uuid.UUID]) -> pd.DataFrame:
        """
        Fetch farm data and convert to DataFrame for training with ALL engineered features.

        Args:
            db: Database session
            farm_data_ids: List of farm data IDs to fetch

        Returns:
            DataFrame with training data and all 14 engineered features
        """
        # Fetch farm data ordered by recorded_at for rolling window calculations
        farm_data = db.query(FarmData).filter(
            FarmData.id.in_(farm_data_ids)
        ).order_by(FarmData.recorded_at).all()

        if not farm_data:
            raise ValueError("No farm data found for training")

        # Convert to list of dicts
        data_list = []
        for record in farm_data:
            data_list.append({
                'temperature': float(record.temperature),
                'ph': float(record.ph),
                'dissolved_oxygen': float(record.dissolved_oxygen),
                'ammonia': float(record.ammonia),
                'nitrate': float(record.nitrate),
                'turbidity': float(record.turbidity),
                # CRITICAL: Database stores weight in KG, but model expects GRAMS
                'fish_weight': float(record.fish_weight) * 1000.0 if record.fish_weight else None,
                'fish_length': float(record.fish_length) if record.fish_length else None,
                'recorded_at': record.recorded_at,
                'start_date': record.start_date,
                'user_id': str(record.user_id),  # Group by user as proxy for pond_id
            })

        df = pd.DataFrame(data_list)

        # Drop rows with missing targets
        df = df.dropna(subset=['fish_weight', 'fish_length'])

        if len(df) == 0:
            raise ValueError("No valid training data after filtering")

        # Sort by timestamp for feature engineering
        df = df.sort_values('recorded_at').reset_index(drop=True)

        # Constants
        SECONDS_PER_DAY = 86400
        OPTIMAL_DO = ModelTrainingService.DEFAULT_CONFIG['preprocessing']['optimal_do']
        ROLLING_WINDOW = ModelTrainingService.DEFAULT_CONFIG['preprocessing']['rolling_window']

        # Feature 1: days_in_farm (days since start_date)
        # Convert start_date (date) to datetime for proper subtraction with recorded_at (Timestamp)
        df['days_in_farm'] = df.apply(
            lambda row: (row['recorded_at'] - pd.to_datetime(row['start_date'])).days if row['start_date'] else 0,
            axis=1
        )

        # Feature 2: day_of_year (1-366)
        df['day_of_year'] = df['recorded_at'].dt.dayofyear

        # Feature 3: hour (0-23)
        df['hour'] = df['recorded_at'].dt.hour

        # Features 4-5: Cyclic encoding of hour (sin_hour, cos_hour)
        df['sin_hour'] = np.sin(2 * np.pi * df['hour'] / 24)
        df['cos_hour'] = np.cos(2 * np.pi * df['hour'] / 24)

        # Feature 6: temp_do_interaction
        df['temp_do_interaction'] = df['temperature'] * df['dissolved_oxygen']

        # Feature 7: avg_do_7d (7-day rolling average of dissolved oxygen)
        # Group by user_id (proxy for pond) for rolling calculations
        df['avg_do_7d'] = df.groupby('user_id')['dissolved_oxygen'].transform(
            lambda x: x.rolling(window=ROLLING_WINDOW, min_periods=1).mean()
        )
        # Fill any NaN with current dissolved_oxygen
        df['avg_do_7d'] = df['avg_do_7d'].fillna(df['dissolved_oxygen'])

        # Feature 8: avg_wqi_7d (Water Quality Index based on DO deviation)
        df['avg_wqi_7d'] = abs(df['avg_do_7d'] - OPTIMAL_DO) / OPTIMAL_DO

        # Drop temporary columns
        df = df.drop(columns=['recorded_at', 'start_date', 'user_id'])

        return df

    @staticmethod
    def _replace_infinite_values(df: pd.DataFrame) -> pd.DataFrame:
        """Replace infinite values with NaN."""
        return df.replace([np.inf, -np.inf], np.nan).copy()

    @staticmethod
    def _remove_multivariate_outliers(df: pd.DataFrame) -> Tuple[pd.DataFrame, int]:
        """Remove multivariate outliers using Isolation Forest on base features."""
        base_features = ['temperature', 'ph', 'dissolved_oxygen', 'ammonia', 'nitrate', 'turbidity']
        features_to_check = [f for f in base_features if f in df.columns]

        if not features_to_check or len(df) < 10:
            return df.copy(), 0

        iso_forest = IsolationForest(contamination=0.01, random_state=42, n_jobs=-1)
        outliers = iso_forest.fit_predict(df[features_to_check])
        outliers_removed = (outliers == -1).sum()

        return df[outliers != -1].copy().reset_index(drop=True), outliers_removed

    @staticmethod
    def _impute_missing_features(df: pd.DataFrame, feature_cols: List[str]) -> pd.DataFrame:
        """Impute missing feature values with median."""
        df_clean = df.copy()
        for col in feature_cols:
            if col in df_clean.columns and df_clean[col].isnull().sum() > 0:
                df_clean[col] = df_clean[col].fillna(df_clean[col].median())
        return df_clean

    @staticmethod
    def _remove_target_outliers(df: pd.DataFrame, target_cols: List[str]) -> Tuple[pd.DataFrame, int]:
        """Remove target outliers using percentile-based filtering (5th-95th)."""
        df_clean = df.copy()
        total_removed = 0

        for col in target_cols:
            if col in df_clean.columns:
                lower = df_clean[col].quantile(0.05)
                upper = df_clean[col].quantile(0.95)
                before = len(df_clean)
                df_clean = df_clean[(df_clean[col] >= lower) & (df_clean[col] <= upper)]
                total_removed += (before - len(df_clean))

        return df_clean.reset_index(drop=True), total_removed

    @staticmethod
    def _remove_duplicates(df: pd.DataFrame) -> Tuple[pd.DataFrame, int]:
        """Remove duplicate rows."""
        duplicates = df.duplicated().sum()
        if duplicates > 0:
            return df.drop_duplicates().copy().reset_index(drop=True), duplicates
        return df.copy(), 0

    @staticmethod
    def preprocess_training_data(
        df: pd.DataFrame,
        feature_cols: List[str],
        target_cols: List[str]
    ) -> pd.DataFrame:
        """
        Apply preprocessing steps to training data (matches notebook preprocessing).

        Pipeline:
        1. Replace infinite values with NaN
        2. Remove multivariate outliers (Isolation Forest on base features)
        3. Impute missing features with median
        4. Remove target outliers (5th-95th percentile)
        5. Remove duplicate rows

        Args:
            df: DataFrame with engineered features
            feature_cols: List of feature column names
            target_cols: List of target column names

        Returns:
            Cleaned DataFrame ready for training
        """
        initial_rows = len(df)

        # Pipeline execution
        df = ModelTrainingService._replace_infinite_values(df)
        df, multivariate_outliers = ModelTrainingService._remove_multivariate_outliers(df)
        df = ModelTrainingService._impute_missing_features(df, feature_cols)
        df, target_outliers = ModelTrainingService._remove_target_outliers(df, target_cols)
        df, duplicates = ModelTrainingService._remove_duplicates(df)

        # Summary
        final_rows = len(df)
        retention = (final_rows / initial_rows * 100) if initial_rows > 0 else 0

        logger.info(
            f"Preprocessing: {initial_rows:,} → {final_rows:,} rows ({retention:.1f}% retained) | "
            f"Removed: {multivariate_outliers:,} multivariate outliers, "
            f"{target_outliers:,} target outliers, {duplicates:,} duplicates"
        )

        # Note: Don't check minimum here - preprocessing is expected to remove some data
        # The minimum check should happen BEFORE preprocessing to give accurate feedback
        if final_rows < 50:
            logger.warning(
                f"Low data after preprocessing: {final_rows} rows. "
                f"Model quality may be degraded. Consider collecting more data."
            )

        return df

    @staticmethod
    def load_base_model(base_model: ModelVersion) -> models.Sequential:
        """
        Load a base model for retraining.

        - If model version is "0.0.0-default": loads from local default path
        - Otherwise: downloads from Cloudinary and loads

        Args:
            base_model: ModelVersion object with model metadata

        Returns:
            Loaded Keras model ready for retraining
        """
        if base_model.version == "0.0.0-default":
            # Load default baseline model from local file
            default_model_path = "app/ml_models/default/baseline_model.keras"
            if not os.path.exists(default_model_path):
                raise ValueError(
                    f"Default baseline model not found at {default_model_path}. "
                    "Please ensure baseline_model.keras exists in the default models directory."
                )

            logger.info(f"Loading default baseline model from {default_model_path}")
            model = tf.keras.models.load_model(default_model_path)
            return model
        else:
            # Download from Cloudinary and load
            if not base_model.keras_model_url:
                raise ValueError(f"Base model {base_model.id} has no Keras model URL")

            logger.info(f"Downloading base model {base_model.version} from cloud storage")

            # Download to temporary file
            import requests
            with tempfile.NamedTemporaryFile(suffix='.keras', delete=False) as tmp_file:
                response = requests.get(base_model.keras_model_url, stream=True)
                response.raise_for_status()

                for chunk in response.iter_content(chunk_size=8192):
                    tmp_file.write(chunk)

                tmp_path = tmp_file.name

            try:
                logger.info(f"Loading model from {tmp_path}")
                model = tf.keras.models.load_model(tmp_path)
                return model
            finally:
                # Clean up temporary file
                if os.path.exists(tmp_path):
                    os.unlink(tmp_path)

    @staticmethod
    def create_model(input_dim: int, output_dim: int = 2, config: Optional[Dict] = None) -> models.Sequential:
        """
        Create neural network model.

        Args:
            input_dim: Number of input features
            output_dim: Number of output targets (default 2: weight and length)
            config: Model configuration dict

        Returns:
            Compiled Keras model
        """
        if config is None:
            config = ModelTrainingService.DEFAULT_CONFIG['model_params']['nn']

        model = models.Sequential([
            layers.Input(shape=(input_dim,)),
            layers.Dense(config['layer_1_units'], kernel_regularizer=l2(config['l2_lambda'])),
            layers.LeakyReLU(),
            layers.BatchNormalization(),
            layers.Dropout(config['dropout_rate_1']),
            layers.Dense(config['layer_2_units'], kernel_regularizer=l2(config['l2_lambda'])),
            layers.LeakyReLU(),
            layers.BatchNormalization(),
            layers.Dropout(config['dropout_rate_2']),
            layers.Dense(config['layer_3_units']),
            layers.LeakyReLU(),
            layers.Dense(output_dim, activation='softplus')
        ])

        model.compile(
            optimizer=Adam(learning_rate=config['learning_rate']),
            loss='mse',
            metrics=['mae']
        )

        return model

    @staticmethod
    def _prepare_model(
        base_model: Optional[ModelVersion],
        input_dim: int,
        learning_rate: float,
        model_config: Dict
    ) -> models.Sequential:
        """
        Prepare model for training (load existing or create new).

        Args:
            base_model: Base model for transfer learning (None for new model)
            input_dim: Number of input features
            learning_rate: Learning rate for optimizer
            model_config: Model configuration dict

        Returns:
            Compiled Keras model
        """
        if base_model:
            # Load and recompile for transfer learning
            model = ModelTrainingService.load_base_model(base_model)
            model.compile(
                optimizer=Adam(learning_rate=learning_rate),
                loss='mse',
                metrics=['mae']
            )
        else:
            # Create new model from scratch
            model = ModelTrainingService.create_model(
                input_dim=input_dim,
                config=model_config
            )

        return model

    @staticmethod
    def _train_with_callbacks(
        model: models.Sequential,
        X_train: np.ndarray,
        y_train: np.ndarray,
        X_val: np.ndarray,
        y_val: np.ndarray,
        epochs: int,
        batch_size: int,
        update_progress
    ):
        """
        Train model with standard callbacks.

        Args:
            model: Compiled Keras model
            X_train: Training features
            y_train: Training targets
            X_val: Validation features
            y_val: Validation targets
            epochs: Number of epochs
            batch_size: Batch size
            update_progress: Progress update function

        Returns:
            Training history
        """
        training_config = ModelTrainingService.DEFAULT_CONFIG['model_params']['training']

        # Setup callbacks
        early_stop = callbacks.EarlyStopping(
            monitor='val_loss',
            patience=training_config['early_stop_patience'],
            restore_best_weights=True
        )
        reduce_lr = callbacks.ReduceLROnPlateau(
            monitor='val_loss',
            factor=training_config['reduce_lr_factor'],
            patience=training_config['reduce_lr_patience'],
            min_lr=training_config['min_lr']
        )

        # Progress tracking callback
        class ProgressCallback(callbacks.Callback):
            def on_epoch_end(self, epoch, logs=None):
                progress = 30.0 + (epoch + 1) / epochs * 50.0
                update_progress(f"Training epoch {epoch + 1}/{epochs}", progress, epoch + 1)

        # Train model
        return model.fit(
            X_train, y_train,
            validation_data=(X_val, y_val),
            epochs=epochs,
            batch_size=batch_size,
            callbacks=[early_stop, reduce_lr, ProgressCallback()],
            verbose=1
        )

    @staticmethod
    def _evaluate_model(
        model: models.Sequential,
        X_test: np.ndarray,
        y_test: np.ndarray,
        history,
        epochs: int
    ) -> Dict[str, Any]:
        """
        Evaluate model and compute metrics.

        Args:
            model: Trained model
            X_test: Test features
            y_test: Test targets
            history: Training history
            epochs: Requested epochs

        Returns:
            Dictionary with training and evaluation metrics
        """
        # Make predictions
        test_pred = model.predict(X_test)

        # Calculate metrics
        r2 = r2_score(y_test, test_pred)
        rmse = np.sqrt(mean_squared_error(y_test, test_pred))
        mae = mean_absolute_error(y_test, test_pred)

        # Training summary
        actual_epochs = len(history.history['loss'])
        early_stopped = actual_epochs < epochs

        return {
            'overall': {
                'r2': float(r2),
                'rmse': float(rmse),
                'mae': float(mae)
            },
            'training': {
                'early_stopped': early_stopped,
                'requested_epochs': epochs,
                'actual_epochs': actual_epochs,
                'best_val_loss': float(min(history.history['val_loss'])),
                'final_train_loss': float(history.history['loss'][-1]),
                'final_val_loss': float(history.history['val_loss'][-1]),
            }
        }

    @staticmethod
    def convert_to_tflite(model: models.Sequential, save_path: str) -> int:
        """
        Convert Keras model to TFLite format.

        Args:
            model: Keras model
            save_path: Path to save TFLite model

        Returns:
            File size in bytes
        """
        converter = tf.lite.TFLiteConverter.from_keras_model(model)
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.target_spec.supported_types = [tf.float16]
        tflite_model = converter.convert()

        with open(save_path, 'wb') as f:
            f.write(tflite_model)

        return os.path.getsize(save_path)

    @staticmethod
    def train_model(
        db: Session,
        base_model_id: Optional[str],
        new_version: str,
        user_id: str,
        epochs: int = 100,
        batch_size: int = 32,
        learning_rate: float = 0.000006,
        notes: Optional[str] = None,
        task_id: Optional[str] = None,
        event_loop = None,
    ) -> ModelVersion:
        """
        Train or retrain a model.

        Args:
            db: Database session
            base_model_id: ID of base model to retrain from (None for initial training)
            new_version: Version string for new model
            user_id: ID of user initiating training
            epochs: Number of training epochs
            batch_size: Batch size
            learning_rate: Learning rate
            notes: Training notes
            task_id: Optional training task ID for progress tracking
            event_loop: Optional asyncio event loop for cross-thread notifications

        Returns:
            ModelVersion object

        Raises:
            ValueError: If validation fails or training errors occur
        """
        start_time = time.time()

        # Helper function to update progress
        def update_progress(stage: str, percentage: float, epoch: Optional[int] = None):
            if task_id:
                from app.models.training_task import TrainingTask
                from app.core.events import training_events
                import asyncio

                task = db.query(TrainingTask).filter(TrainingTask.id == task_id).first()
                if task:
                    task.current_stage = stage
                    task.progress_percentage = percentage
                    if epoch is not None:
                        task.current_epoch = epoch
                        task.total_epochs = epochs
                    db.commit()

                    # Notify SSE streams of the update from background thread
                    # Only attempt if event loop was provided (running in background thread)
                    if event_loop:
                        try:
                            # Schedule coroutine on the event loop from background thread
                            asyncio.run_coroutine_threadsafe(
                                training_events.notify_training_updated(str(task_id)),
                                event_loop
                            )
                        except Exception as e:
                            # Log but don't fail training if notification fails
                            logger.warning(f"Failed to notify training update: {e}")

        # Validate base model if provided
        update_progress("Validating base model", 5.0)
        base_model = None
        if base_model_id:
            base_model = db.query(ModelVersion).filter(ModelVersion.id == base_model_id).first()
            if not base_model:
                raise ValueError(f"Base model {base_model_id} not found")
            if base_model.status != ModelStatus.COMPLETED:
                raise ValueError(f"Base model must be in COMPLETED status, got {base_model.status}")

        # Check if version already exists
        existing = db.query(ModelVersion).filter(ModelVersion.version == new_version).first()
        if existing:
            raise ValueError(f"Model version {new_version} already exists")

        # Get unused farm data
        update_progress("Fetching training data", 10.0)
        unused_data_ids = ModelTrainingService.get_unused_farm_data(db)
        if len(unused_data_ids) < 100:  # Minimum samples check
            raise ValueError(f"Insufficient unused farm data: {len(unused_data_ids)} samples (minimum 100 required)")

        # Fetch training data with engineered features
        update_progress("Processing features", 15.0)
        df = ModelTrainingService.fetch_training_data(db, unused_data_ids)

        # Check data sufficiency BEFORE preprocessing
        # This gives accurate feedback about the actual data available
        initial_row_count = len(df)
        if initial_row_count < 100:
            raise ValueError(
                f"Insufficient data for training: {initial_row_count} samples available "
                f"(minimum 100 required before preprocessing). "
                f"Please sync more verified farm data entries."
            )

        logger.info(f"Starting with {initial_row_count} samples before preprocessing")

        # Prepare features and targets
        feature_cols = ModelTrainingService.DEFAULT_CONFIG['feature_cols']
        target_cols = ModelTrainingService.DEFAULT_CONFIG['target_cols']

        # Apply preprocessing (clean data, remove outliers, handle missing values)
        update_progress("Cleaning and preprocessing data", 17.0)
        df = ModelTrainingService.preprocess_training_data(df, feature_cols, target_cols)

        X = df[feature_cols].values
        y = df[target_cols].values

        # Split data
        update_progress("Splitting and scaling data", 20.0)
        config = ModelTrainingService.DEFAULT_CONFIG['preprocessing']
        X_temp, X_test, y_temp, y_test = train_test_split(
            X, y, test_size=config['test_size'], random_state=config['random_state']
        )
        val_size = config['val_size'] / (1 - config['test_size'])
        X_train, X_val, y_train, y_val = train_test_split(
            X_temp, y_temp, test_size=val_size, random_state=config['random_state']
        )

        # Scale features using RobustScaler fit on new training data
        update_progress("Scaling features", 22.0)
        scaler = RobustScaler()
        X_train_scaled = scaler.fit_transform(X_train)
        X_val_scaled = scaler.transform(X_val)
        X_test_scaled = scaler.transform(X_test)

        logger.info(f"Data scaled - Features: {X_train_scaled.shape}, "
                   f"Targets: weight(mean={y_train[:, 0].mean():.1f}g, std={y_train[:, 0].std():.1f}g), "
                   f"length(mean={y_train[:, 1].mean():.1f}cm, std={y_train[:, 1].std():.1f}cm)")

        # Load or create model
        model_config = ModelTrainingService.DEFAULT_CONFIG['model_params']['nn'].copy()
        model_config['learning_rate'] = learning_rate

        # Prepare model (load for transfer learning or create new)
        update_progress("Preparing model", 25.0)
        model = ModelTrainingService._prepare_model(
            base_model=base_model,
            input_dim=X_train_scaled.shape[1],
            learning_rate=learning_rate,
            model_config=model_config
        )

        if base_model:
            logger.info(f"Transfer learning from {base_model.version} (lr={learning_rate})")
            model_config['loaded_from'] = base_model.version
        else:
            logger.info(f"Training new model from scratch (lr={learning_rate})")
            model_config['loaded_from'] = None

        # Train model with callbacks
        update_progress("Training model", 30.0)
        history = ModelTrainingService._train_with_callbacks(
            model=model,
            X_train=X_train_scaled,
            y_train=y_train,
            X_val=X_val_scaled,
            y_val=y_val,
            epochs=epochs,
            batch_size=batch_size,
            update_progress=update_progress
        )

        # Evaluate model and compute metrics
        update_progress("Evaluating model", 82.0)
        metrics = ModelTrainingService._evaluate_model(
            model=model,
            X_test=X_test_scaled,
            y_test=y_test,
            history=history,
            epochs=epochs
        )

        logger.info(f"Training complete: {metrics['training']['actual_epochs']} epochs, "
                   f"R²={metrics['overall']['r2']:.4f}, RMSE={metrics['overall']['rmse']:.1f}, "
                   f"MAE={metrics['overall']['mae']:.1f}")

        # Create temporary directory for model files
        update_progress("Converting and saving models", 85.0)
        with tempfile.TemporaryDirectory() as temp_dir:
            # Save Keras model
            keras_path = os.path.join(temp_dir, f'model_v{new_version}.keras')
            model.save(keras_path)

            # Convert to TFLite
            tflite_path = os.path.join(temp_dir, f'model_v{new_version}.tflite')
            ModelTrainingService.convert_to_tflite(model, tflite_path)

            # Save scaler as pickle file
            scaler_path = os.path.join(temp_dir, f'scaler_v{new_version}.pkl')
            joblib.dump(scaler, scaler_path)

            # Save preprocessing config (now includes scaler URL)
            preprocessing_config = {
                'feature_names': feature_cols,
                'target_columns': target_cols,
                'optimal_do': ModelTrainingService.DEFAULT_CONFIG['preprocessing']['optimal_do'],
                'rolling_window': ModelTrainingService.DEFAULT_CONFIG['preprocessing']['rolling_window']
            }

            # Upload models to Cloudinary
            update_progress("Uploading models to cloud storage", 90.0)
            tflite_result = cloudinary_storage.upload_model(
                tflite_path, new_version, "tflite"
            )
            keras_result = cloudinary_storage.upload_model(
                keras_path, new_version, "keras"
            )
            scaler_result = cloudinary_storage.upload_model(
                scaler_path, new_version, "scaler"
            )

            # Add scaler URL to preprocessing config
            preprocessing_config['scaler_url'] = scaler_result['url']
            preprocessing_config['scaler_cloudinary_id'] = scaler_result['cloudinary_id']

        # Calculate training duration
        training_duration = int(time.time() - start_time)

        # Create ModelVersion record
        update_progress("Saving model metadata", 95.0)
        model_version = ModelVersion(
            version=new_version,
            tflite_model_url=tflite_result['url'],
            keras_model_url=keras_result['url'],
            tflite_size_bytes=tflite_result['size_bytes'],
            keras_size_bytes=keras_result['size_bytes'],
            tflite_cloudinary_id=tflite_result['cloudinary_id'],
            keras_cloudinary_id=keras_result['cloudinary_id'],
            base_model_id=base_model_id,
            preprocessing_config=preprocessing_config,
            model_config=model_config,
            training_data_count=len(unused_data_ids),
            training_duration_seconds=training_duration,
            trained_by=user_id,
            metrics=metrics,
            status=ModelStatus.COMPLETED,
            is_deployed=False,
            is_active=True,
            notes=notes,
        )

        db.add(model_version)
        db.flush()  # Get model_version.id

        # Create TrainingSession record
        training_session = TrainingSession(
            model_version_id=model_version.id,
            farm_data_ids=unused_data_ids,
            training_samples=len(X_train),
            validation_samples=len(X_val),
            test_samples=len(X_test),
            epochs=len(history.history['loss']),
            batch_size=batch_size,
            learning_rate=learning_rate,
            final_metrics=metrics,
            training_history={
                'loss': [float(x) for x in history.history['loss']],
                'val_loss': [float(x) for x in history.history['val_loss']],
                'mae': [float(x) for x in history.history['mae']],
                'val_mae': [float(x) for x in history.history['val_mae']],
                'early_stopped': metrics['training']['early_stopped'],
                'requested_epochs': metrics['training']['requested_epochs'],
                'actual_epochs': metrics['training']['actual_epochs'],
            },
            started_at=datetime.utcfromtimestamp(start_time),
            completed_at=datetime.utcnow(),
        )

        db.add(training_session)
        db.commit()
        db.refresh(model_version)

        return model_version
