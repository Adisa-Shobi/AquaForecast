export type ModelStatus = 'training' | 'completed' | 'failed' | 'deployed' | 'archived';

export interface ModelVersion {
  model_id: string;
  version: string;
  tflite_model_url: string;
  keras_model_url: string | null;
  tflite_size_bytes: number | null;
  keras_size_bytes: number | null;
  base_model_id: string | null;
  base_model_version: string | null;
  preprocessing_config: Record<string, any> | null;
  model_config: Record<string, any> | null;
  training_data_count: number | null;
  training_duration_seconds: number | null;
  metrics: ModelMetrics | null;
  status: ModelStatus;
  is_deployed: boolean;
  is_active: boolean;
  min_app_version: string | null;
  created_at: string;
  deployed_at: string | null;
  notes: string | null;
}

export interface ModelMetrics {
  overall: {
    r2: number;
    rmse: number;
    mae: number;
  };
}

export interface ModelListResponse {
  models: ModelVersion[];
  total_count: number;
  deployed_model_id: string | null;
}

export interface ModelMetricsDetail {
  model_id: string;
  version: string;
  metrics: ModelMetrics;
  training_data_count: number;
  training_duration_seconds: number | null;
  training_history: {
    loss: number[];
    val_loss: number[];
    mae: number[];
    val_mae: number[];
  } | null;
  created_at: string;
}

export interface RetrainRequest {
  base_model_id: string;
  new_version: string;
  notes?: string;
  epochs?: number;
  batch_size?: number;
  learning_rate?: number;
}

export interface DeployRequest {
  model_id: string;
  notes?: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
}

// Training task tracking
export type TrainingTaskStatus = 'pending' | 'running' | 'completed' | 'failed';

export interface TrainingTask {
  task_id: string;
  base_model_id: string | null;
  new_version: string;
  initiated_by: string;
  status: TrainingTaskStatus;
  progress_percentage: number;
  current_epoch: number | null;
  total_epochs: number | null;
  current_stage: string | null;
  error_message: string | null;
  result_model_id: string | null;
  training_params: Record<string, any> | null;
  created_at: string;
  started_at: string | null;
  completed_at: string | null;
}

export interface TrainingTaskListResponse {
  tasks: TrainingTask[];
  total_count: number;
}

export interface RetrainResponse {
  message: string;
  task_id: string;
  new_version: string;
  base_model_version: string | null;
  estimated_duration_minutes: number;
}

export interface DeleteModelResponse {
  model_id: string;
  version: string;
  deleted_training_sessions: number;
  deleted_training_tasks: number;
  storage_files_deleted: string[];
  storage_errors: string[] | null;
  message: string;
}
