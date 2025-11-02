import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { modelApi } from '../api/client';
import type { ModelVersion } from '../types/model';
import { ArrowLeft, Rocket, AlertTriangle } from 'lucide-react';

export default function DeployModel() {
  const { modelId } = useParams<{ modelId: string }>();
  const navigate = useNavigate();
  const [model, setModel] = useState<ModelVersion | null>(null);
  const [deployedModel, setDeployedModel] = useState<ModelVersion | null>(null);
  const [loading, setLoading] = useState(true);
  const [deploying, setDeploying] = useState(false);
  const [notes, setNotes] = useState('');

  useEffect(() => {
    loadData();
  }, [modelId]);

  const loadData = async () => {
    try {
      setLoading(true);
      const [allModels, deployed] = await Promise.all([
        modelApi.listModels(false),
        modelApi.getDeployedModel().catch(() => null),
      ]);

      const selectedModel = allModels.models.find(m => m.model_id === modelId);
      setModel(selectedModel || null);
      setDeployedModel(deployed);
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDeploy = async () => {
    if (!model) return;

    if (!confirm('Are you sure you want to deploy this model? This will replace the current deployed model.')) {
      return;
    }

    try {
      setDeploying(true);
      await modelApi.deployModel({
        model_id: model.model_id,
        notes,
      });
      alert('Model deployed successfully!');
      navigate('/');
    } catch (error: any) {
      alert(`Failed to deploy model: ${error.response?.data?.detail || error.message}`);
    } finally {
      setDeploying(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!model) {
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

  const metricsImproved = deployedModel && model.metrics && deployedModel.metrics
    ? model.metrics.overall.r2 > deployedModel.metrics.overall.r2
    : null;

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate('/')}
          className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
        >
          <ArrowLeft size={24} />
        </button>
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Deploy Model</h1>
          <p className="text-gray-600 mt-1">Deploy v{model.version} to production</p>
        </div>
      </div>

      {/* Warning if deployed model exists */}
      {deployedModel && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 flex gap-3">
          <AlertTriangle className="text-yellow-600 flex-shrink-0" size={20} />
          <div className="text-sm text-yellow-800">
            <p className="font-medium mb-1">Current Deployed Model</p>
            <p>
              Version <strong>v{deployedModel.version}</strong> is currently deployed.
              Deploying this model will automatically undeploy it.
            </p>
          </div>
        </div>
      )}

      {/* Model Comparison */}
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <div className="px-6 py-4 bg-gray-50 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">Model Comparison</h2>
        </div>
        <div className="p-6">
          <div className="grid grid-cols-2 gap-6">
            {/* New Model */}
            <div>
              <h3 className="text-sm font-medium text-gray-500 mb-3">New Model (v{model.version})</h3>
              <div className="space-y-2">
                <div className="flex justify-between">
                  <span className="text-sm text-gray-600">Overall R²</span>
                  <span className="text-sm font-semibold text-gray-900">
                    {model.metrics ? (model.metrics.overall.r2 * 100).toFixed(2) : 'N/A'}%
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-gray-600">RMSE</span>
                  <span className="text-sm font-semibold text-gray-900">
                    {model.metrics ? model.metrics.overall.rmse.toFixed(4) : 'N/A'}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-gray-600">Training Data</span>
                  <span className="text-sm font-semibold text-gray-900">
                    {model.training_data_count?.toLocaleString() || 'N/A'}
                  </span>
                </div>
              </div>
            </div>

            {/* Current Deployed Model */}
            <div>
              <h3 className="text-sm font-medium text-gray-500 mb-3">
                {deployedModel ? `Current (v${deployedModel.version})` : 'No Deployed Model'}
              </h3>
              {deployedModel ? (
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-600">Overall R²</span>
                    <span className="text-sm font-semibold text-gray-900">
                      {deployedModel.metrics ? (deployedModel.metrics.overall.r2 * 100).toFixed(2) : 'N/A'}%
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-600">RMSE</span>
                    <span className="text-sm font-semibold text-gray-900">
                      {deployedModel.metrics ? deployedModel.metrics.overall.rmse.toFixed(4) : 'N/A'}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-gray-600">Training Data</span>
                    <span className="text-sm font-semibold text-gray-900">
                      {deployedModel.training_data_count?.toLocaleString() || 'N/A'}
                    </span>
                  </div>
                </div>
              ) : (
                <p className="text-sm text-gray-500">No model currently deployed</p>
              )}
            </div>
          </div>

          {/* Improvement Indicator */}
          {metricsImproved !== null && (
            <div className={`mt-4 p-3 rounded-lg ${metricsImproved ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
              <p className="text-sm font-medium">
                {metricsImproved
                  ? '✓ This model shows improved performance'
                  : '⚠ This model has lower performance than current'}
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Deployment Notes */}
      <div className="bg-white rounded-lg shadow p-6">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Deployment Notes (Optional)
        </label>
        <textarea
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          rows={4}
          placeholder="Add deployment notes (e.g., reason for deployment, changes made)..."
          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />
      </div>

      {/* Deploy Button */}
      <div className="flex gap-4">
        <button
          onClick={handleDeploy}
          disabled={deploying}
          className="flex-1 bg-purple-600 text-white px-6 py-3 rounded-lg hover:bg-purple-700 transition-colors flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {deploying ? (
            <>
              <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
              Deploying...
            </>
          ) : (
            <>
              <Rocket size={20} />
              Deploy to Production
            </>
          )}
        </button>
        <button
          onClick={() => navigate('/')}
          className="px-6 py-3 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
        >
          Cancel
        </button>
      </div>
    </div>
  );
}
