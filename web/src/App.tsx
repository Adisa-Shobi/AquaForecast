import { BrowserRouter, Routes, Route, Link, useLocation, useNavigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import ModelDetails from './pages/ModelDetails';
import RetrainModel from './pages/RetrainModel';
import DeployModel from './pages/DeployModel';
import { LogOut } from 'lucide-react';

function Layout({ children }: { children: React.ReactNode }) {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, signOut } = useAuth();

  const handleSignOut = async () => {
    await signOut();
    localStorage.removeItem('access_code_verified');
    navigate('/login');
  };

  return (
    <div className="flex flex-col min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <Link to="/" className="flex items-center gap-3">
              <img
                src="/logo.png"
                alt="AquaForecast Logo"
                className="w-10 h-10 rounded-lg"
              />
              <div>
                <h1 className="text-xl font-bold text-gray-900">AquaForecast</h1>
                <p className="text-xs text-gray-500">Model Management</p>
              </div>
            </Link>

            <div className="flex items-center gap-6">
              <nav className="flex gap-6">
                <Link
                  to="/"
                  className={`text-sm font-medium transition-colors ${
                    location.pathname === '/'
                      ? 'text-blue-600'
                      : 'text-gray-600 hover:text-gray-900'
                  }`}
                >
                  Dashboard
                </Link>
                <Link
                  to="/retrain"
                  className={`text-sm font-medium transition-colors ${
                    location.pathname === '/retrain'
                      ? 'text-blue-600'
                      : 'text-gray-600 hover:text-gray-900'
                  }`}
                >
                  Retrain
                </Link>
              </nav>

              {user && (
                <div className="flex items-center gap-3">
                  <span className="text-sm text-gray-600">{user.email}</span>
                  <button
                    onClick={handleSignOut}
                    className="text-gray-600 hover:text-gray-900 transition-colors"
                    title="Sign out"
                  >
                    <LogOut size={20} />
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full">
        {children}
      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-gray-200 mt-auto">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <p className="text-center text-sm text-gray-500">
            © 2025 AquaForecast. Model Management Dashboard.
          </p>
        </div>
      </footer>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/*"
            element={
              <ProtectedRoute>
                <Layout>
                  <Routes>
                    <Route path="/" element={<Dashboard />} />
                    <Route path="/models/:modelId" element={<ModelDetails />} />
                    <Route path="/retrain" element={<RetrainModel />} />
                    <Route path="/deploy/:modelId" element={<DeployModel />} />
                  </Routes>
                </Layout>
              </ProtectedRoute>
            }
          />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
