import axios from 'axios';
import type {
  ApiResponse,
  ModelListResponse,
  ModelVersion,
  ModelMetricsDetail,
  RetrainRequest,
  DeployRequest,
  RetrainResponse,
  TrainingTask,
  TrainingTaskListResponse,
} from '../types/model';
import { auth } from '../config/firebase';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use(
  async (config) => {
    const user = auth.currentUser;
    if (user) {
      const token = await user.getIdToken();
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export const setAuthToken = (token: string | null) => {
  if (token) {
    apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`;
  } else {
    delete apiClient.defaults.headers.common['Authorization'];
  }
};

export const modelApi = {
  // List all models
  async listModels(includeArchived: boolean = false): Promise<ModelListResponse> {
    const { data } = await apiClient.get<ApiResponse<ModelListResponse>>(
      `/models/list?include_archived=${includeArchived}`
    );
    return data.data;
  },

  // Get deployed model
  async getDeployedModel(): Promise<ModelVersion> {
    const { data } = await apiClient.get<ApiResponse<ModelVersion>>('/models/deployed');
    return data.data;
  },

  // Get model metrics
  async getModelMetrics(modelId: string): Promise<ModelMetricsDetail> {
    const { data } = await apiClient.get<ApiResponse<ModelMetricsDetail>>(
      `/models/${modelId}/metrics`
    );
    return data.data;
  },

  // Retrain model
  async retrainModel(request: RetrainRequest): Promise<RetrainResponse> {
    const { data} = await apiClient.post<ApiResponse<RetrainResponse>>('/models/retrain', request);
    return data.data;
  },

  // Get training tasks
  async getTrainingTasks(status?: string, limit?: number): Promise<TrainingTaskListResponse> {
    const params = new URLSearchParams();
    if (status) params.append('status', status);
    if (limit) params.append('limit', limit.toString());

    const { data } = await apiClient.get<ApiResponse<TrainingTaskListResponse>>(
      `/models/training/tasks?${params.toString()}`
    );
    return data.data;
  },

  // Get training task status
  async getTrainingTaskStatus(taskId: string): Promise<TrainingTask> {
    const { data } = await apiClient.get<ApiResponse<TrainingTask>>(
      `/models/training/tasks/${taskId}`
    );
    return data.data;
  },

  // Deploy model
  async deployModel(request: DeployRequest): Promise<ModelVersion> {
    const { data } = await apiClient.post<ApiResponse<ModelVersion>>('/models/deploy', request);
    return data.data;
  },

  // Archive model
  async archiveModel(modelId: string): Promise<any> {
    const { data } = await apiClient.delete<ApiResponse<any>>(`/models/${modelId}/archive`);
    return data.data;
  },
};

export default apiClient;
