import { useEffect, useState, useRef } from 'react';
import { modelApi } from '../api/client';
import type { TrainingTask } from '../types/model';
import { Loader2, CheckCircle, XCircle, Clock, WifiOff } from 'lucide-react';
import { auth } from '../config/firebase';
import { EventSourceWithHeaders } from '../utils/EventSource';

export default function TrainingProgress() {
  const [tasks, setTasks] = useState<TrainingTask[]>([]);
  const [loading, setLoading] = useState(true);
  const [connectionStatus, setConnectionStatus] = useState<'connected' | 'disconnected' | 'error'>('disconnected');
  const eventSourceRef = useRef<EventSourceWithHeaders | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    // Initial load
    loadInitialTasks();

    // Connect to SSE stream
    connectToSSE();

    return () => {
      // Cleanup
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
    };
  }, []);

  const loadInitialTasks = async () => {
    try {
      const data = await modelApi.getTrainingTasks(undefined, 10);
      setTasks(data.tasks);
    } catch (error) {
      console.error('Failed to load initial training tasks:', error);
    } finally {
      setLoading(false);
    }
  };

  const connectToSSE = async () => {
    try {
      // Get Firebase token for authentication
      const user = auth.currentUser;
      if (!user) {
        console.error('No authenticated user for SSE connection');
        setConnectionStatus('error');
        return;
      }

      const token = await user.getIdToken();

      // Close existing connection
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }

      // Create SSE connection with Authorization header (secure!)
      const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api/v1';
      const url = `${apiBaseUrl}/models/training/tasks/stream`;
      const eventSource = new EventSourceWithHeaders(url, {
        'Authorization': `Bearer ${token}`,
      });

      eventSource.onopen = () => {
        console.log('SSE connection established');
        setConnectionStatus('connected');
      };

      eventSource.onmessage = (message) => {
        try {
          const taskUpdate: TrainingTask = JSON.parse(message.data);

          // Update or add task in state
          setTasks((prevTasks) => {
            const existingIndex = prevTasks.findIndex(t => t.task_id === taskUpdate.task_id);
            if (existingIndex >= 0) {
              // Update existing task
              const newTasks = [...prevTasks];
              newTasks[existingIndex] = taskUpdate;
              return newTasks;
            } else {
              // Add new task at the beginning
              return [taskUpdate, ...prevTasks].slice(0, 10); // Keep max 10 tasks
            }
          });
        } catch (error) {
          console.error('Failed to parse SSE message:', error);
        }
      };

      eventSource.onerror = (error) => {
        console.error('SSE connection error:', error);
        setConnectionStatus('error');
        eventSource.close();

        // Attempt reconnection after 5 seconds
        reconnectTimeoutRef.current = setTimeout(() => {
          console.log('Attempting to reconnect SSE...');
          connectToSSE();
        }, 5000);
      };

      eventSource.connect();

      eventSourceRef.current = eventSource;
    } catch (error) {
      console.error('Failed to connect to SSE:', error);
      setConnectionStatus('error');
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'running':
        return <Loader2 className="animate-spin text-blue-600" size={20} />;
      case 'completed':
        return <CheckCircle className="text-green-600" size={20} />;
      case 'failed':
        return <XCircle className="text-red-600" size={20} />;
      case 'pending':
        return <Clock className="text-gray-600" size={20} />;
      default:
        return null;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'running':
        return 'bg-blue-100 text-blue-800';
      case 'completed':
        return 'bg-green-100 text-green-800';
      case 'failed':
        return 'bg-red-100 text-red-800';
      case 'pending':
        return 'bg-gray-100 text-gray-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const formatDuration = (startedAt: string | null, completedAt: string | null) => {
    if (!startedAt) return 'Not started';
    if (!completedAt) return 'In progress...';

    const start = new Date(startedAt).getTime();
    const end = new Date(completedAt).getTime();
    const durationMinutes = Math.round((end - start) / 60000);

    if (durationMinutes < 1) return '< 1 minute';
    if (durationMinutes < 60) return `${durationMinutes} minutes`;
    const hours = Math.floor(durationMinutes / 60);
    const mins = durationMinutes % 60;
    return `${hours}h ${mins}m`;
  };

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center justify-center">
          <Loader2 className="animate-spin text-gray-400" size={24} />
          <span className="ml-2 text-gray-600">Loading training tasks...</span>
        </div>
      </div>
    );
  }

  if (tasks.length === 0) {
    return null; // Don't show if no tasks
  }

  return (
    <div className="bg-white rounded-lg shadow">
      <div className="p-6 border-b border-gray-200">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl font-bold text-gray-900">Training Tasks</h2>
            <p className="text-sm text-gray-600 mt-1">Real-time training progress monitoring</p>
          </div>
          {connectionStatus === 'connected' && (
            <div className="flex items-center gap-2 text-sm text-green-600">
              <div className="w-2 h-2 bg-green-600 rounded-full animate-pulse"></div>
              Live updates
            </div>
          )}
          {connectionStatus === 'error' && (
            <div className="flex items-center gap-2 text-sm text-amber-600">
              <WifiOff size={16} />
              Reconnecting...
            </div>
          )}
        </div>
      </div>

      <div className="divide-y divide-gray-200">
        {tasks.map((task) => (
          <div key={task.task_id} className="p-6">
            <div className="flex items-start justify-between">
              <div className="flex-1">
                <div className="flex items-center gap-3">
                  {getStatusIcon(task.status)}
                  <h3 className="font-semibold text-gray-900">
                    Model v{task.new_version}
                  </h3>
                  <span className={`px-2 py-1 text-xs font-medium rounded ${getStatusColor(task.status)}`}>
                    {task.status.toUpperCase()}
                  </span>
                </div>

                {task.current_stage && (
                  <p className="text-sm text-gray-600 mt-2">{task.current_stage}</p>
                )}

                {task.status === 'running' && (
                  <div className="mt-3">
                    <div className="flex items-center justify-between text-sm text-gray-600 mb-1">
                      <span>
                        {task.current_epoch && task.total_epochs
                          ? `Epoch ${task.current_epoch}/${task.total_epochs}`
                          : 'Processing...'}
                      </span>
                      <span>{task.progress_percentage.toFixed(1)}%</span>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-2">
                      <div
                        className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                        style={{ width: `${task.progress_percentage}%` }}
                      />
                    </div>
                  </div>
                )}

                {task.error_message && (
                  <div className="mt-3 p-3 bg-red-50 border border-red-200 rounded text-sm text-red-800">
                    <strong>Error:</strong> {task.error_message}
                  </div>
                )}

                <div className="mt-3 flex flex-wrap gap-4 text-xs text-gray-500">
                  <span>Started: {new Date(task.created_at).toLocaleString()}</span>
                  <span>Duration: {formatDuration(task.started_at, task.completed_at)}</span>
                  {task.training_params?.epochs && (
                    <span>Epochs: {task.training_params.epochs}</span>
                  )}
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
