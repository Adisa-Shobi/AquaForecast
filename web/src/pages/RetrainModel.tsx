import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { modelApi } from '../api/client';
import type { ModelVersion, RetrainRequest } from '../types/model';
import { ArrowLeft, Play, Info } from 'lucide-react';

export default function RetrainModel() {
  const navigate = useNavigate();
  const [models, setModels] = useState<ModelVersion[]>([]);
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState<RetrainRequest>({
    base_model_id: '',
    new_version: '',
    notes: '',
    epochs: 100,
    batch_size: 32,
    learning_rate: 0.000006,
  });

  useEffect(() => {
    loadModels();
  }, []);

  const loadModels = async () => {
    try {
      const data = await modelApi.listModels(false);
      // Filter only completed models
      setModels(data.models.filter(m => m.status === 'completed'));
    } catch (error) {
      console.error('Failed to load models:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.base_model_id || !formData.new_version) {
      alert('Please select a base model and enter a version');
      return;
    }

    try {
      setLoading(true);
      const response = await modelApi.retrainModel(formData);
      alert(
        `Training started successfully!\n\n` +
        `Version: ${response.new_version}\n` +
        `Task ID: ${response.task_id}\n` +
        `Estimated duration: ~${response.estimated_duration_minutes} minutes\n\n` +
        `Check the dashboard to track progress.`
      );
      navigate('/', { state: { refresh: true } });
    } catch (error: any) {
      alert(`Failed to start retraining: ${error.response?.data?.detail || error.message}`);
    } finally {
      setLoading(false);
    }
  };

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
          <h1 className="text-3xl font-bold text-gray-900">Retrain Model</h1>
          <p className="text-gray-600 mt-1">Train a new model from an existing base model</p>
        </div>
      </div>

      {/* Info Box */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 flex gap-3">
        <Info className="text-blue-600 flex-shrink-0" size={20} />
        <div className="text-sm text-blue-800">
          <p className="font-medium mb-1">About Model Retraining</p>
          <ul className="list-disc list-inside space-y-1">
            <li>Uses only unused farm data (no data reuse)</li>
            <li>Minimum 100 samples required</li>
            <li>Training runs in background (check dashboard for status)</li>
            <li>All 14 features engineered automatically</li>
          </ul>
        </div>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow p-6 space-y-6">
        {/* Base Model Selection */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Base Model *
          </label>
          <select
            value={formData.base_model_id}
            onChange={(e) => setFormData({ ...formData, base_model_id: e.target.value })}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            required
          >
            <option value="">Select a base model...</option>
            {models.map((model) => (
              <option key={model.model_id} value={model.model_id}>
                v{model.version} - R²: {model.metrics ? (model.metrics.overall.r2 * 100).toFixed(2) : 'N/A'}%
                {model.is_deployed && ' (Deployed)'}
              </option>
            ))}
          </select>
          <p className="mt-1 text-xs text-gray-500">
            Select the model to use as starting point for retraining
          </p>
        </div>

        {/* New Version */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            New Version *
          </label>
          <input
            type="text"
            value={formData.new_version}
            onChange={(e) => setFormData({ ...formData, new_version: e.target.value })}
            placeholder="e.g., 1.3.0"
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            required
          />
          <p className="mt-1 text-xs text-gray-500">
            Use semantic versioning (MAJOR.MINOR.PATCH)
          </p>
        </div>

        {/* Training Parameters */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Epochs
            </label>
            <input
              type="number"
              value={formData.epochs}
              onChange={(e) => setFormData({ ...formData, epochs: parseInt(e.target.value) })}
              min="1"
              max="500"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Batch Size
            </label>
            <input
              type="number"
              value={formData.batch_size}
              onChange={(e) => setFormData({ ...formData, batch_size: parseInt(e.target.value) })}
              min="8"
              max="256"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Learning Rate
            </label>
            <input
              type="number"
              value={formData.learning_rate}
              onChange={(e) => setFormData({ ...formData, learning_rate: parseFloat(e.target.value) })}
              step="0.000001"
              min="0.000001"
              max="0.1"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
        </div>

        {/* Notes */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Notes (Optional)
          </label>
          <textarea
            value={formData.notes}
            onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
            rows={4}
            placeholder="Add training notes or release notes..."
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>

        {/* Submit Button */}
        <div className="flex gap-4">
          <button
            type="submit"
            disabled={loading}
            className="flex-1 bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? (
              <>
                <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                Starting Training...
              </>
            ) : (
              <>
                <Play size={20} />
                Start Retraining
              </>
            )}
          </button>
          <button
            type="button"
            onClick={() => navigate('/')}
            className="px-6 py-3 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
