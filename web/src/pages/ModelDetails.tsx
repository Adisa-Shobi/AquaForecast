import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { modelApi } from '../api/client';
import type { ModelVersion, ModelMetricsDetail } from '../types/model';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { ArrowLeft, Download, TrendingUp, Clock, Database } from 'lucide-react';

export default function ModelDetails() {
  const { modelId } = useParams<{ modelId: string }>();
  const navigate = useNavigate();
  const [metrics, setMetrics] = useState<ModelMetricsDetail | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (modelId) {
      loadMetrics();
    }
  }, [modelId]);

  const loadMetrics = async () => {
    try {
      setLoading(true);
      const data = await modelApi.getModelMetrics(modelId!);
      setMetrics(data);
    } catch (error) {
      console.error('Failed to load metrics:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatDuration = (seconds: number | null) => {
    if (!seconds) return 'N/A';
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    if (hours > 0) {
      return `${hours}h ${minutes}m`;
    }
    return `${minutes}m ${seconds % 60}s`;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!metrics) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500">Model not found</p>
        <button
          onClick={() => navigate('/')}
          className="mt-4 text-blue-600 hover:text-blue-800"
        >
          Back to Dashboard
        </button>
      </div>
    );
  }

  // Prepare training history data
  const trainingData = metrics.training_history
    ? metrics.training_history.loss.map((_, index) => ({
        epoch: index + 1,
        loss: metrics.training_history!.loss[index],
        val_loss: metrics.training_history!.val_loss[index],
        mae: metrics.training_history!.mae[index],
        val_mae: metrics.training_history!.val_mae[index],
      }))
    : [];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate('/')}
          className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
        >
          <ArrowLeft size={24} />
        </button>
        <div className="flex-1">
          <h1 className="text-3xl font-bold text-gray-900">Model v{metrics.version}</h1>
          <p className="text-gray-600 mt-1">Detailed metrics and training information</p>
        </div>
      </div>

      {/* Quick Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white p-6 rounded-lg shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Overall R²</p>
              <p className="text-3xl font-bold text-blue-600">
                {(metrics.metrics.overall.r2 * 100).toFixed(2)}%
              </p>
            </div>
            <TrendingUp className="text-blue-600" size={40} />
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Training Data</p>
              <p className="text-3xl font-bold text-green-600">
                {metrics.training_data_count.toLocaleString()}
              </p>
            </div>
            <Database className="text-green-600" size={40} />
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Training Time</p>
              <p className="text-3xl font-bold text-purple-600">
                {formatDuration(metrics.training_duration_seconds)}
              </p>
            </div>
            <Clock className="text-purple-600" size={40} />
          </div>
        </div>
      </div>

      {/* Detailed Metrics */}
      <div className="bg-white p-6 rounded-lg shadow">
        <h2 className="text-xl font-bold text-gray-900 mb-4">Model Metrics</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="flex flex-col items-center p-4 bg-blue-50 rounded-lg">
            <span className="text-sm font-medium text-gray-700 mb-2">R² Score</span>
            <span className="text-3xl font-bold text-blue-600">
              {metrics.metrics.overall.r2.toFixed(4)}
            </span>
          </div>
          <div className="flex flex-col items-center p-4 bg-gray-50 rounded-lg">
            <span className="text-sm font-medium text-gray-700 mb-2">RMSE</span>
            <span className="text-3xl font-bold text-gray-900">
              {metrics.metrics.overall.rmse.toFixed(4)}
            </span>
          </div>
          <div className="flex flex-col items-center p-4 bg-gray-50 rounded-lg">
            <span className="text-sm font-medium text-gray-700 mb-2">MAE</span>
            <span className="text-3xl font-bold text-gray-900">
              {metrics.metrics.overall.mae.toFixed(4)}
            </span>
          </div>
        </div>
      </div>

      {/* Training History Charts */}
      {trainingData.length > 0 && (
        <div className="bg-white p-6 rounded-lg shadow space-y-6">
          <h2 className="text-xl font-bold text-gray-900">Training History</h2>

          {/* Loss Chart */}
          <div>
            <h3 className="text-lg font-semibold text-gray-700 mb-3">Loss over Epochs</h3>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={trainingData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="epoch" label={{ value: 'Epoch', position: 'insideBottom', offset: -5 }} />
                <YAxis label={{ value: 'Loss', angle: -90, position: 'insideLeft' }} />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="loss" stroke="#3B82F6" name="Training Loss" />
                <Line type="monotone" dataKey="val_loss" stroke="#EF4444" name="Validation Loss" />
              </LineChart>
            </ResponsiveContainer>
          </div>

          {/* MAE Chart */}
          <div>
            <h3 className="text-lg font-semibold text-gray-700 mb-3">MAE over Epochs</h3>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={trainingData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="epoch" label={{ value: 'Epoch', position: 'insideBottom', offset: -5 }} />
                <YAxis label={{ value: 'MAE', angle: -90, position: 'insideLeft' }} />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="mae" stroke="#10B981" name="Training MAE" />
                <Line type="monotone" dataKey="val_mae" stroke="#F59E0B" name="Validation MAE" />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}
    </div>
  );
}
