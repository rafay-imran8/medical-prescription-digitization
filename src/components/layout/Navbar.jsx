import { Link, useNavigate } from 'react-router-dom';
// import { useAuth } from '../../context/AuthContext';
import { useAuth } from '../../hooks/useAuth';

import { useState } from 'react';
import FeedbackModal from '../common/FeedbackModal';
import { 
  FileText, 
  LogOut, 
  User, 
  Users, 
  Settings,
  Home,
  Activity,
  MessageSquare
} from 'lucide-react';

const Navbar = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
 const [feedbackModal, setFeedbackModal] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const getDashboardLink = () => {
    switch (user?.role) {
      case 'PATIENT':
        return '/patient/dashboard';
      case 'DOCTOR':
        return '/doctor/dashboard';
      case 'ADMIN':
        return '/admin/dashboard';
      case 'ANALYST':
        return '/analyst/dashboard';
      default:
        return '/';
    }
  };

  return (
    <>
        <nav className="bg-white shadow-sm border-b border-gray-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          {/* Logo and Brand */}
          <div className="flex items-center">
            <Link to={getDashboardLink()} className="flex items-center space-x-2">
              <FileText className="h-8 w-8 text-primary-600" />
              <span className="text-xl font-bold text-gray-900">
                Prescription System
              </span>
            </Link>
          </div>

          {/* Navigation Links */}
          <div className="flex items-center space-x-4">
            {user && (
              <>
                <Link
                  to={getDashboardLink()}
                  className="flex items-center space-x-1 text-gray-700 hover:text-primary-600 transition-colors"
                >
                  <Home className="h-5 w-5" />
                  <span>Dashboard</span>
                </Link>

                {user.role === 'PATIENT' && (
                  <Link
                    to="/patient/access"
                    className="flex items-center space-x-1 text-gray-700 hover:text-primary-600 transition-colors"
                  >
                    <Users className="h-5 w-5" />
                    <span>Access</span>
                  </Link>
                )}

                {user.role === 'DOCTOR' && (
                  <Link
                    to="/doctor/patients"
                    className="flex items-center space-x-1 text-gray-700 hover:text-primary-600 transition-colors"
                  >
                    <Users className="h-5 w-5" />
                    <span>Patients</span>
                  </Link>
                )}

             {user.role === 'ADMIN' && (
  <>
    <Link
      to="/admin/users"
      className="flex items-center space-x-1 text-gray-700 hover:text-primary-600 transition-colors"
    >
      <Settings className="h-5 w-5" />
      <span>Manage Users</span>
    </Link>
    <Link
      to="/admin/feedback"
      className="flex items-center space-x-1 text-gray-700 hover:text-primary-600 transition-colors"
    >
      <MessageSquare className="h-5 w-5" />
      <span>Feedback</span>
    </Link>
    <Link
      to="/admin/logs"
      className="flex items-center space-x-1 text-gray-700 hover:text-primary-600 transition-colors"
    >
      <Activity className="h-5 w-5" />
      <span>Logs</span>
    </Link>
  </>
)}

                {/* User Menu */}
                <div className="flex items-center space-x-3 border-l pl-4">
                  <div className="text-right">
                    <p className="text-sm font-medium text-gray-900">
                      {user.fullName}
                    </p>
                    <p className="text-xs text-gray-500">
                      {user.role}
                    </p>
                  </div>
                  <button
          onClick={() => setFeedbackModal(true)}
          className="flex items-center space-x-1 text-gray-700 hover:text-primary-600 transition-colors"
          title="Feedback"
        >
          <MessageSquare className="h-5 w-5" />
          <span>Feedback</span>
        </button>
                  <button
                    onClick={handleLogout}
                    className="flex items-center space-x-1 text-gray-700 hover:text-red-600 transition-colors"
                    title="Logout"
                  >
                    <LogOut className="h-5 w-5" />
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </nav>
          <FeedbackModal isOpen={feedbackModal} onClose={() => setFeedbackModal(false)} />

    </>
  );
};

export default Navbar;