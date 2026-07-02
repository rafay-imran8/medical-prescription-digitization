// src/pages/auth/SetPassword.jsx
import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Lock, Eye, EyeOff, CheckCircle, XCircle } from 'lucide-react';
import { authAPI } from '/src/services/api.js';
import toast from 'react-hot-toast';

export default function SetPassword() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [loading, setLoading] = useState(false);
  const [validating, setValidating] = useState(true);
  const [tokenValid, setTokenValid] = useState(false);
  const [userName, setUserName] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [formData, setFormData] = useState({
    password: '',
    confirmPassword: ''
  });
  const [passwordStrength, setPasswordStrength] = useState({
    hasLength: false,
    hasUpper: false,
    hasLower: false,
    hasNumber: false,
    hasSpecial: false
  });

  useEffect(() => {
    if (!token) {
      toast.error('Invalid or missing token');
      navigate('/login');
      return;
    }
    validateToken();
  }, [token]);

  useEffect(() => {
    checkPasswordStrength(formData.password);
  }, [formData.password]);

  const validateToken = async () => {
    try {
      const response = await authAPI.validateToken(token);
      setTokenValid(true);
      setUserName(response.data.fullName);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Invalid or expired token');
      setTokenValid(false);
      setTimeout(() => navigate('/login'), 3000);
    } finally {
      setValidating(false);
    }
  };

  const checkPasswordStrength = (password) => {
    setPasswordStrength({
      hasLength: password.length >= 8,
      hasUpper: /[A-Z]/.test(password),
      hasLower: /[a-z]/.test(password),
      hasNumber: /\d/.test(password),
      hasSpecial: /[@$!%*?&#]/.test(password)
    });
  };

  const isPasswordStrong = () => {
    return Object.values(passwordStrength).every(value => value === true);
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!isPasswordStrong()) {
      toast.error('Please meet all password requirements');
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      toast.error('Passwords do not match');
      return;
    }

    setLoading(true);
    try {
      await authAPI.setPassword({
        token,
        password: formData.password,
        confirmPassword: formData.confirmPassword
      });

      toast.success('Password set successfully! Redirecting to login...');
      
      setTimeout(() => {
        navigate('/login');
      }, 2000);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to set password');
    } finally {
      setLoading(false);
    }
  };

  if (validating) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Validating token...</p>
        </div>
      </div>
    );
  }

  if (!tokenValid) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="bg-white shadow-lg rounded-lg p-8 max-w-md w-full text-center">
          <XCircle size={48} className="text-red-600 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Invalid Token</h2>
          <p className="text-gray-600 mb-4">
            This password setup link is invalid or has expired.
          </p>
          <button
            onClick={() => navigate('/login')}
            className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700"
          >
            Go to Login
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full">
        <div className="bg-white shadow-lg rounded-lg p-8">
          {/* Header */}
          <div className="text-center mb-6">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <Lock size={32} className="text-green-600" />
            </div>
            <h2 className="text-2xl font-bold text-gray-900">Set Your Password</h2>
            <p className="text-sm text-gray-600 mt-2">
              Welcome, <strong>{userName}</strong>!
            </p>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Password */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                New Password
              </label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  required
                  className="w-full px-4 py-2 pr-10 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="Enter password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-500 hover:text-gray-700"
                >
                  {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                </button>
              </div>
            </div>

            {/* Confirm Password */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Confirm Password
              </label>
              <div className="relative">
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  name="confirmPassword"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  required
                  className="w-full px-4 py-2 pr-10 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="Confirm password"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-500 hover:text-gray-700"
                >
                  {showConfirmPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                </button>
              </div>
            </div>

            {/* Password Requirements */}
            <div className="bg-gray-50 rounded-lg p-4">
              <p className="text-sm font-medium text-gray-900 mb-3">Password Requirements:</p>
              <div className="space-y-2">
                <div className={`flex items-center gap-2 text-sm ${passwordStrength.hasLength ? 'text-green-600' : 'text-gray-500'}`}>
                  {passwordStrength.hasLength ? <CheckCircle size={16} /> : <XCircle size={16} />}
                  At least 8 characters
                </div>
                <div className={`flex items-center gap-2 text-sm ${passwordStrength.hasUpper ? 'text-green-600' : 'text-gray-500'}`}>
                  {passwordStrength.hasUpper ? <CheckCircle size={16} /> : <XCircle size={16} />}
                  One uppercase letter
                </div>
                <div className={`flex items-center gap-2 text-sm ${passwordStrength.hasLower ? 'text-green-600' : 'text-gray-500'}`}>
                  {passwordStrength.hasLower ? <CheckCircle size={16} /> : <XCircle size={16} />}
                  One lowercase letter
                </div>
                <div className={`flex items-center gap-2 text-sm ${passwordStrength.hasNumber ? 'text-green-600' : 'text-gray-500'}`}>
                  {passwordStrength.hasNumber ? <CheckCircle size={16} /> : <XCircle size={16} />}
                  One number
                </div>
                <div className={`flex items-center gap-2 text-sm ${passwordStrength.hasSpecial ? 'text-green-600' : 'text-gray-500'}`}>
                  {passwordStrength.hasSpecial ? <CheckCircle size={16} /> : <XCircle size={16} />}
                  One special character (@$!%*?&#)
                </div>
              </div>
            </div>

            {/* Password Match Indicator */}
            {formData.confirmPassword && (
              <div className={`text-sm ${formData.password === formData.confirmPassword ? 'text-green-600' : 'text-red-600'}`}>
                {formData.password === formData.confirmPassword ? (
                  <div className="flex items-center gap-2">
                    <CheckCircle size={16} />
                    Passwords match
                  </div>
                ) : (
                  <div className="flex items-center gap-2">
                    <XCircle size={16} />
                    Passwords do not match
                  </div>
                )}
              </div>
            )}

            {/* Submit Button */}
            <button
              type="submit"
              disabled={loading || !isPasswordStrong() || formData.password !== formData.confirmPassword}
              className="w-full bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                  Setting Password...
                </>
              ) : (
                'Set Password & Activate Account'
              )}
            </button>
          </form>

          {/* Info */}
          <div className="mt-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p className="text-sm text-blue-800">
              <strong>🔒 Security Tip:</strong> Choose a strong, unique password that you don't use elsewhere.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}