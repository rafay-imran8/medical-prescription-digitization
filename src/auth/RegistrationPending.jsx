// src/pages/auth/RegistrationPending.jsx
import { useNavigate } from 'react-router-dom';
import { Clock, CheckCircle, Mail } from 'lucide-react';

export default function RegistrationPending() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full">
        <div className="bg-white shadow-lg rounded-lg p-8 text-center">
          {/* Icon */}
          <div className="flex items-center justify-center mb-6">
            <div className="w-20 h-20 bg-blue-100 rounded-full flex items-center justify-center">
              <Clock size={40} className="text-blue-600" />
            </div>
          </div>

          {/* Title */}
          <h2 className="text-2xl font-bold text-gray-900 mb-4">
            Registration Submitted!
          </h2>

          {/* Description */}
          <p className="text-gray-600 mb-6">
            Thank you for registering with our Prescription Management System.
          </p>

          {/* Status Box */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 mb-6">
            <div className="flex items-start gap-3 mb-4">
              <Mail size={24} className="text-blue-600 mt-1 flex-shrink-0" />
              <div className="text-left">
                <p className="text-sm font-medium text-blue-900 mb-1">
                  Confirmation Email Sent
                </p>
                <p className="text-sm text-blue-700">
                  Check your inbox for a confirmation email.
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <Clock size={24} className="text-blue-600 mt-1 flex-shrink-0" />
              <div className="text-left">
                <p className="text-sm font-medium text-blue-900 mb-1">
                  Under Review
                </p>
                <p className="text-sm text-blue-700">
                  Your registration is being reviewed by our admin team.
                </p>
              </div>
            </div>
          </div>

          {/* Timeline */}
          <div className="bg-gray-50 rounded-lg p-6 mb-6">
            <h3 className="text-sm font-semibold text-gray-900 mb-4">What Happens Next?</h3>
            <div className="space-y-4 text-left">
              <div className="flex items-start gap-3">
                <div className="flex items-center justify-center w-6 h-6 bg-blue-600 text-white rounded-full text-xs font-bold flex-shrink-0 mt-0.5">
                  1
                </div>
                <div className="flex-1">
                  <p className="text-sm font-medium text-gray-900">Admin Review</p>
                  <p className="text-xs text-gray-600">
                    Our team will verify your information (24-48 hours)
                  </p>
                </div>
              </div>

              <div className="flex items-start gap-3">
                <div className="flex items-center justify-center w-6 h-6 bg-blue-600 text-white rounded-full text-xs font-bold flex-shrink-0 mt-0.5">
                  2
                </div>
                <div className="flex-1">
                  <p className="text-sm font-medium text-gray-900">Email Notification</p>
                  <p className="text-xs text-gray-600">
                    You'll receive an email with a password setup link
                  </p>
                </div>
              </div>

              <div className="flex items-start gap-3">
                <div className="flex items-center justify-center w-6 h-6 bg-blue-600 text-white rounded-full text-xs font-bold flex-shrink-0 mt-0.5">
                  3
                </div>
                <div className="flex-1">
                  <p className="text-sm font-medium text-gray-900">Set Password</p>
                  <p className="text-xs text-gray-600">
                    Click the link in the email to set your password
                  </p>
                </div>
              </div>

              <div className="flex items-start gap-3">
                <div className="flex items-center justify-center w-6 h-6 bg-green-600 text-white rounded-full text-xs font-bold flex-shrink-0 mt-0.5">
                  <CheckCircle size={16} />
                </div>
                <div className="flex-1">
                  <p className="text-sm font-medium text-gray-900">Start Using</p>
                  <p className="text-xs text-gray-600">
                    Log in and start managing prescriptions
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Important Note */}
          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-6">
            <p className="text-sm text-yellow-800">
              <strong>⚠️ Important:</strong> You cannot log in until your account is approved and you've set your password.
            </p>
          </div>

          {/* Action Buttons */}
          <div className="space-y-3">
            <button
              onClick={() => navigate('/login')}
              className="w-full bg-blue-600 text-white py-2.5 rounded-lg hover:bg-blue-700 font-medium"
            >
              Go to Login
            </button>
            <button
              onClick={() => navigate('/')}
              className="w-full bg-gray-100 text-gray-700 py-2.5 rounded-lg hover:bg-gray-200 font-medium"
            >
              Back to Home
            </button>
          </div>

          {/* Help Text */}
          <p className="text-xs text-gray-500 mt-6">
            Didn't receive an email? Check your spam folder or contact support.
          </p>
        </div>
      </div>
    </div>
  );
}