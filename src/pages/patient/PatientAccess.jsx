// src/pages/patient/PatientAccess.jsx
import { useState, useEffect } from 'react';
import { 
  Shield, CheckCircle, XCircle, Clock, Mail, Copy, 
  AlertCircle, Key, User 
} from 'lucide-react';
import { patientAPI } from '../../services/api';
import toast from 'react-hot-toast';

export default function PatientAccess() {
  const [pendingRequests, setPendingRequests] = useState([]);
  const [grantedAccess, setGrantedAccess] = useState([]);
  const [loading, setLoading] = useState(true);
  const [processingId, setProcessingId] = useState(null);
  const [showCodeModal, setShowCodeModal] = useState(false);
  const [currentCode, setCurrentCode] = useState(null);

  useEffect(() => {
    fetchAccessData();
  }, []);

  const fetchAccessData = async () => {
    try {
      const [pendingRes, grantedRes] = await Promise.all([
        patientAPI.getPendingAccessRequests(),
        patientAPI.getGrantedDoctors()
      ]);
      setPendingRequests(pendingRes.data);
      setGrantedAccess(grantedRes.data);
    } catch (error) {
      toast.error('Failed to load access data');
    } finally {
      setLoading(false);
    }
  };

  const handleGrantAccess = async (doctorId, doctorName) => {
    setProcessingId(doctorId);
    try {
      const response = await patientAPI.grantAccessWithCode(doctorId);
      
      // Show the code in modal
      setCurrentCode({
        code: response.data.accessCode,
        doctorName: doctorName,
        expiresAt: response.data.expiresAt
      });
      setShowCodeModal(true);
      
      toast.success('Access granted! Code displayed below.');
      
      // Refresh lists
      fetchAccessData();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to grant access');
    } finally {
      setProcessingId(null);
    }
  };

  const handleRevokeAccess = async (doctorId) => {
    if (!confirm('Are you sure you want to revoke access for this doctor?')) {
      return;
    }

    setProcessingId(doctorId);
    try {
      await patientAPI.revokeAccess(doctorId);
      toast.success('Access revoked successfully');
      fetchAccessData();
    } catch (error) {
      toast.error('Failed to revoke access');
    } finally {
      setProcessingId(null);
    }
  };

  const copyCodeToClipboard = () => {
    if (currentCode?.code) {
      navigator.clipboard.writeText(currentCode.code);
      toast.success('Code copied to clipboard!');
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-white rounded-lg shadow p-6">
        <h1 className="text-2xl font-bold flex items-center gap-2">
          <Shield size={28} />
          Manage Doctor Access
        </h1>
        <p className="text-gray-600 mt-2">
          Control which doctors can view your medical prescriptions
        </p>
      </div>

      {/* Pending Requests */}
      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-xl font-semibold mb-4 flex items-center gap-2">
          <Clock size={24} className="text-yellow-600" />
          Pending Access Requests
          {pendingRequests.length > 0 && (
            <span className="ml-2 px-2 py-1 bg-yellow-100 text-yellow-800 text-sm rounded-full">
              {pendingRequests.length}
            </span>
          )}
        </h2>

        {pendingRequests.length > 0 ? (
          <div className="space-y-3">
            {pendingRequests.map((request) => (
              <div
                key={request.doctorId}
                className="border border-yellow-200 bg-yellow-50 rounded-lg p-4"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 bg-yellow-200 rounded-full flex items-center justify-center">
                      <User size={24} className="text-yellow-700" />
                    </div>
                    <div>
                      <h3 className="font-semibold">Dr. {request.doctorName}</h3>
                      <p className="text-sm text-gray-600">{request.specialization}</p>
                      <p className="text-xs text-gray-500 mt-1">
                        ID: {request.doctorUniqueId}
                      </p>
                    </div>
                  </div>

                  <div className="flex gap-2">
                    <button
                      onClick={() => handleGrantAccess(request.doctorId, request.doctorName)}
                      disabled={processingId === request.doctorId}
                      className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:bg-gray-400 flex items-center gap-2"
                    >
                      {processingId === request.doctorId ? (
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                      ) : (
                        <CheckCircle size={18} />
                      )}
                      Approve
                    </button>
                    <button
                      onClick={() => handleRevokeAccess(request.doctorId)}
                      disabled={processingId === request.doctorId}
                      className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:bg-gray-400 flex items-center gap-2"
                    >
                      <XCircle size={18} />
                      Deny
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-8 text-gray-500">
            <Clock size={48} className="mx-auto mb-3 text-gray-300" />
            <p>No pending access requests</p>
          </div>
        )}
      </div>

      {/* Granted Access */}
      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-xl font-semibold mb-4 flex items-center gap-2">
          <CheckCircle size={24} className="text-green-600" />
          Granted Access
          {grantedAccess.length > 0 && (
            <span className="ml-2 px-2 py-1 bg-green-100 text-green-800 text-sm rounded-full">
              {grantedAccess.length}
            </span>
          )}
        </h2>

        {grantedAccess.length > 0 ? (
          <div className="space-y-3">
            {grantedAccess.map((access) => (
              <div
                key={access.doctorId}
                className="border border-green-200 bg-green-50 rounded-lg p-4"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 bg-green-200 rounded-full flex items-center justify-center">
                      <User size={24} className="text-green-700" />
                    </div>
                    <div>
                      <h3 className="font-semibold">Dr. {access.doctorName}</h3>
                      <p className="text-sm text-gray-600">{access.specialization}</p>
                      <p className="text-xs text-gray-500 mt-1">
                        Granted: {new Date(access.accessGrantedAt).toLocaleDateString()}
                      </p>
                      {access.accessExpiresAt && (
                        <p className="text-xs text-gray-500">
                          Expires: {new Date(access.accessExpiresAt).toLocaleString()}
                        </p>
                      )}
                    </div>
                  </div>

                  <button
                    onClick={() => handleRevokeAccess(access.doctorId)}
                    disabled={processingId === access.doctorId}
                    className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:bg-gray-400 flex items-center gap-2"
                  >
                    {processingId === access.doctorId ? (
                      <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                    ) : (
                      <XCircle size={18} />
                    )}
                    Revoke
                  </button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-8 text-gray-500">
            <Shield size={48} className="mx-auto mb-3 text-gray-300" />
            <p>No doctors have been granted access</p>
          </div>
        )}
      </div>

      {/* Access Code Modal */}
      {showCodeModal && currentCode && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg max-w-md w-full p-6">
            <div className="text-center mb-6">
              <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <Key size={32} className="text-green-600" />
              </div>
              <h3 className="text-xl font-bold mb-2">Access Code Generated</h3>
              <p className="text-gray-600 text-sm">
                Share this code with Dr. {currentCode.doctorName}
              </p>
            </div>

            <div className="bg-green-50 border-2 border-green-200 rounded-lg p-6 mb-4">
              <div className="flex items-center justify-between mb-2">
                <span className="text-sm text-green-700">Access Code:</span>
                <button
                  onClick={copyCodeToClipboard}
                  className="text-green-600 hover:text-green-700 flex items-center gap-1 text-sm"
                >
                  <Copy size={16} />
                  Copy
                </button>
              </div>
              <div className="text-4xl font-mono font-bold text-center text-green-900 tracking-widest">
                {currentCode.code}
              </div>
            </div>

            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-4">
              <div className="flex items-start gap-2">
                <AlertCircle size={18} className="text-yellow-600 mt-0.5 flex-shrink-0" />
                <div className="text-sm text-yellow-800">
                  <p className="font-medium mb-1">Important:</p>
                  <ul className="list-disc list-inside space-y-1">
                    <li>This code has been sent to your email</li>
                    <li>Code expires in 1 hour</li>
                    <li>Once doctor enters the code, access is valid for 24 hours</li>
                    <li>You can revoke access anytime</li>
                  </ul>
                </div>
              </div>
            </div>

            <button
              onClick={() => setShowCodeModal(false)}
              className="w-full bg-blue-600 text-white py-3 rounded-lg hover:bg-blue-700"
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
}