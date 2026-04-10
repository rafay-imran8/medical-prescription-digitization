// src/pages/doctor/DoctorAccessCodeEntry.jsx
import { useState } from 'react';
import { Key, CheckCircle, AlertCircle } from 'lucide-react';
import { doctorAPI } from '../../services/api';
import toast from 'react-hot-toast';

export default function DoctorAccessCodeEntry() {
  const [accessCode, setAccessCode] = useState('');
  const [activating, setActivating] = useState(false);

  const handleActivateAccess = async (e) => {
    e.preventDefault();

    if (!accessCode || accessCode.length !== 6) {
      toast.error('Please enter a valid 6-digit access code');
      return;
    }

    setActivating(true);
    try {
      const response = await doctorAPI.activateAccess(accessCode);
      toast.success(response.data.message || 'Access activated successfully!');
      setAccessCode('');
      
      // Redirect to patients list after 2 seconds
      setTimeout(() => {
        window.location.href = '/doctor/patients';
      }, 2000);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Invalid or expired code');
    } finally {
      setActivating(false);
    }
  };

  const handleCodeChange = (e) => {
    const value = e.target.value.replace(/\D/g, '').slice(0, 6);
    setAccessCode(value);
  };

  return (
    <div className="max-w-md mx-auto">
      <div className="bg-white rounded-lg shadow p-8">
        <div className="text-center mb-6">
          <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <Key size={32} className="text-blue-600" />
          </div>
          <h2 className="text-2xl font-bold mb-2">Activate Patient Access</h2>
          <p className="text-gray-600">
            Enter the 6-digit code sent to the patient's email
          </p>
        </div>

        <form onSubmit={handleActivateAccess} className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Access Code
            </label>
            <input
              type="text"
              value={accessCode}
              onChange={handleCodeChange}
              placeholder="000000"
              maxLength={6}
              className="w-full px-4 py-3 text-center text-2xl font-mono tracking-widest border-2 border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              autoFocus
            />
            <p className="text-xs text-gray-500 mt-2 text-center">
              Code is valid for 1 hour
            </p>
          </div>

          <button
            type="submit"
            disabled={accessCode.length !== 6 || activating}
            className="w-full bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            {activating ? (
              <>
                <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                Activating...
              </>
            ) : (
              <>
                <CheckCircle size={20} />
                Activate Access
              </>
            )}
          </button>
        </form>

        <div className="mt-6 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
          <div className="flex items-start gap-2">
            <AlertCircle size={18} className="text-yellow-600 mt-0.5 flex-shrink-0" />
            <div className="text-sm text-yellow-800">
              <p className="font-medium mb-1">Important:</p>
              <ul className="list-disc list-inside space-y-1">
                <li>The patient receives this code via email after approving your request</li>
                <li>Code expires in 1 hour</li>
                <li>Once activated, access is valid for 24 hours</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}