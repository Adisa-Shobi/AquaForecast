import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { modelApi } from '../api/client';
import type { ModelListResponse, ModelVersion } from '../types/model';
import {
  CheckCircle,
  Clock,
  XCircle,
  Archive,
  Rocket,
  TrendingUp,
  Database,
  Calendar,
  Trash2
} from 'lucide-react';
import { formatDistance } from 'date-fns';
import TrainingProgress from '../components/TrainingProgress';

const STATUS_COLORS = {
  training: 'bg-blue-100 text-blue-800',
  completed: 'bg-green-100 text-green-800',
  failed: 'bg-red-100 text-red-800',
  deployed: 'bg-purple-100 text-purple-800',
  archived: 'bg-gray-100 text-gray-800',
};

const STATUS_ICONS = {
  training: Clock,
  completed: CheckCircle,
  failed: XCircle,
  deployed: Rocket,
  archived: Archive,
};

export default function Dashboard() {
  const [models, setModels] = useState<ModelVersion[]>([]);
  const [deployedModelId, setDeployedModelId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [includeArchived, setIncludeArchived] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [modelToDelete, setModelToDelete] = useState<ModelVersion | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    loadModels();
  }, [includeArchived]);

  // Refresh when navigating back with refresh state
  useEffect(() => {
    if (location.state?.refresh) {
      loadModels();
      // Clear the state to prevent refresh on next visit
      window.history.replaceState({}, document.title);
    }
  }, [location.state]);

  // Refresh data when component becomes visible
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (!document.hidden) {
        loadModels();
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange);
  }, [includeArchived]);

  const loadModels = async () => {
    try {
      setLoading(true);
      const data = await modelApi.listModels(includeArchived);
      setModels(data.models);
      setDeployedModelId(data.deployed_model_id);
    } catch (error) {
      console.error('Failed to load models:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatBytes = (bytes: number | null) => {
    if (!bytes) return 'N/A';
    const mb = bytes / (1024 * 1024);
    return `${mb.toFixed(2)} MB`;
  };

  const formatDuration = (seconds: number | null) => {
    if (!seconds) return 'N/A';
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}m ${remainingSeconds}s`;
  };

  const handleDeleteClick = (model: ModelVersion) => {
    setModelToDelete(model);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!modelToDelete) return;

    try {
      setDeleting(true);
      await modelApi.deleteModel(modelToDelete.model_id, true);
      setDeleteDialogOpen(false);
      setModelToDelete(null);
      await loadModels(); // Refresh the list
      alert(`Model ${modelToDelete.version} deleted successfully`);
    } catch (error: any) {
      console.error('Failed to delete model:', error);
      const errorMessage = error.response?.data?.detail || error.message || 'Failed to delete model';
      alert(`Error: ${errorMessage}`);
    } finally {
      setDeleting(false);
    }
  };

  const handleDeleteCancel = () => {
    setDeleteDialogOpen(false);
    setModelToDelete(null);
  };;

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Model Management</h1>
          <p className="text-gray-600 mt-1">
            Manage ML models, view metrics, and deploy to production
          </p>
        </div>
        <button
          onClick={() => navigate('/retrain')}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
        >
          <TrendingUp size={20} />
          Retrain Model
        </button>
      </div>

      {/* Training Progress */}
      <TrainingProgress onTaskComplete={() => loadModels()} />

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white p-6 rounded-lg shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Total Models</p>
              <p className="text-2xl font-bold text-gray-900">{models.length}</p>
            </div>
            <Database className="text-blue-600" size={32} />
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Deployed</p>
              <p className="text-2xl font-bold text-purple-600">
                {models.filter(m => m.is_deployed).length}
              </p>
            </div>
            <Rocket className="text-purple-600" size={32} />
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Completed</p>
              <p className="text-2xl font-bold text-green-600">
                {models.filter(m => m.status === 'completed').length}
              </p>
            </div>
            <CheckCircle className="text-green-600" size={32} />
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Training</p>
              <p className="text-2xl font-bold text-blue-600">
                {models.filter(m => m.status === 'training').length}
              </p>
            </div>
            <Clock className="text-blue-600" size={32} />
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white p-4 rounded-lg shadow">
        <label className="flex items-center gap-2 cursor-pointer">
          <input
            type="checkbox"
            checked={includeArchived}
            onChange={(e) => setIncludeArchived(e.target.checked)}
            className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500"
          />
          <span className="text-sm text-gray-700">Include archived models</span>
        </label>
      </div>

      {/* Model List */}
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Version
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Status
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Metrics (R²)
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Data Count
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Size
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Created
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {models.map((model) => {
              const StatusIcon = STATUS_ICONS[model.status];
              return (
                <tr
                  key={model.model_id}
                  className={`hover:bg-gray-50 ${model.is_deployed ? 'bg-purple-50' : ''}`}
                >
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <div>
                        <div className="text-sm font-medium text-gray-900">
                          {model.version}
                          {model.is_deployed && (
                            <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-purple-100 text-purple-800">
                              <Rocket size={12} className="mr-1" />
                              Deployed
                            </span>
                          )}
                        </div>
                        {model.base_model_version && (
                          <div className="text-xs text-gray-500">
                            from v{model.base_model_version}
                          </div>
                        )}
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${STATUS_COLORS[model.status]}`}>
                      <StatusIcon size={14} className="mr-1" />
                      {model.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {model.metrics?.overall.r2
                      ? `${(model.metrics.overall.r2 * 100).toFixed(2)}%`
                      : 'N/A'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {model.training_data_count?.toLocaleString() || 'N/A'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {formatBytes(model.tflite_size_bytes)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <div className="flex items-center gap-1">
                      <Calendar size={14} />
                      {formatDistance(new Date(model.created_at), new Date(), { addSuffix: true })}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2">
                    <button
                      onClick={() => navigate(`/models/${model.model_id}`)}
                      className="text-blue-600 hover:text-blue-900"
                    >
                      View
                    </button>
                    {model.status === 'completed' && !model.is_deployed && (
                      <button
                        onClick={() => navigate(`/deploy/${model.model_id}`)}
                        className="text-purple-600 hover:text-purple-900"
                      >
                        Deploy
                      </button>
                    )}
                    {!model.is_deployed && (
                      <button
                        onClick={() => handleDeleteClick(model)}
                        className="text-red-600 hover:text-red-900"
                        title="Delete model"
                      >
                        Delete
                      </button>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>

        {models.length === 0 && (
          <div className="text-center py-12 text-gray-500">
            No models found. Start by retraining a model.
          </div>
        )}
      </div>

      {/* Delete Confirmation Dialog */}
      {deleteDialogOpen && modelToDelete && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <div className="flex items-center gap-3 mb-4">
              <div className="flex-shrink-0 w-10 h-10 rounded-full bg-red-100 flex items-center justify-center">
                <Trash2 className="text-red-600" size={20} />
              </div>
              <h3 className="text-lg font-semibold text-gray-900">Delete Model</h3>
            </div>

            <div className="mb-6">
              <p className="text-gray-700 mb-2">
                Are you sure you want to delete model <strong>{modelToDelete.version}</strong>?
              </p>
              <p className="text-sm text-gray-600 mb-3">
                This will permanently delete:
              </p>
              <ul className="text-sm text-gray-600 space-y-1 ml-4">
                <li>• Model files (TFLite, Keras)</li>
                <li>• All training sessions</li>
                <li>• Training tasks for this model</li>
                <li>• Cloud storage files</li>
              </ul>
              <p className="text-sm text-red-600 font-medium mt-3">
                WARNING: This action cannot be undone!
              </p>
            </div>

            <div className="flex gap-3">
              <button
                onClick={handleDeleteCancel}
                disabled={deleting}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                onClick={handleDeleteConfirm}
                disabled={deleting}
                className="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
              >
                {deleting ? (
                  <>
                    <div className="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent"></div>
                    Deleting...
                  </>
                ) : (
                  <>
                    <Trash2 size={16} />
                    Delete
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
