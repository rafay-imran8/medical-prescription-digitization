// src/pages/admin/AdminRegistrations.jsx
import { useState, useEffect } from 'react';
import { 
  Clock, CheckCircle, XCircle, User, Mail, 
  CreditCard, FileText, Download, Eye 
} from 'lucide-react';
import { adminAPI } from '../../services/api';
import toast from 'react-hot-toast';

export default function AdminRegistrations() {
  const [registrations, setRegistrations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedUser, setSelectedUser] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [rejectReason, setRejectReason] = useState('');

  useEffect(() => {
    fetchPendingRegistrations();
  }, []);

  const fetchPendingRegistrations = async () => {
    try {
      const response = await adminAPI.getPendingRegistrations();
      setRegistrations(response.data.data);
    } catch (error) {
      toast.error('Failed to load registrations');
    } finally {
      setLoading(false);
    }
  };

  const handleViewDetails = async (userId) => {
    try {
      const response = await adminAPI.getUserDetails(userId);
      setSelectedUser(response.data.data);
      setShowModal(true);
    } catch (error) {
      toast.error('Failed to load user details');
    }
  };

  const handleApprove = async (userId, notes = '') => {
    setActionLoading(true);
    try {
      await adminAPI.approveRegistration({
        userId,
        approved: true,
        notes
      });
      toast.success('Registration approved! Password setup email sent.');
      fetchPendingRegistrations();
      setShowModal(false);
      setSelectedUser(null);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to approve');
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async (userId) => {
    if (!rejectReason.trim()) {
      toast.error('Please provide a rejection reason');
      return;
    }

    setActionLoading(true);
    try {
      await adminAPI.rejectRegistration({
        userId,
        approved: false,
        rejectionReason: rejectReason
      });
      toast.success('Registration rejected. Notification sent to user.');
      fetchPendingRegistrations();
      setShowModal(false);
      setSelectedUser(null);
      setRejectReason('');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to reject');
    } finally {
      setActionLoading(false);
    }
  };

  const getRoleBadgeColor = (role) => {
    const colors = {
      PATIENT: 'bg-blue-100 text-blue-800',
      DOCTOR: 'bg-purple-100 text-purple-800'
    };
    return colors[role] || 'bg-gray-100 text-gray-800';
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
          <Clock size={28} className="text-yellow-600" />
          Pending Registrations
          {registrations.length > 0 && (
            <span className="ml-2 px-3 py-1 bg-yellow-100 text-yellow-800 text-sm rounded-full">
              {registrations.length}
            </span>
          )}
        </h1>
        <p className="text-gray-600 mt-2">
          Review and approve/reject user registrations
        </p>
      </div>

      {/* Registrations List */}
      {registrations.length > 0 ? (
        <div className="grid gap-4">
          {registrations.map((user) => (
            <div
              key={user.userId}
              className="bg-white rounded-lg shadow p-6 hover:shadow-md transition-shadow"
            >
              <div className="flex items-start justify-between">
                <div className="flex items-start gap-4 flex-1">
                  <div className="w-14 h-14 bg-yellow-100 rounded-full flex items-center justify-center flex-shrink-0">
                    <User size={28} className="text-yellow-700" />
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <h3 className="text-lg font-semibold">{user.fullName}</h3>
                      <span className={`px-3 py-1 rounded-full text-xs font-medium ${getRoleBadgeColor(user.role)}`}>
                        {user.role}
                      </span>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-2 text-sm text-gray-600">
                      <div className="flex items-center gap-2">
                        <Mail size={16} />
                        {user.email}
                      </div>
                      <div className="flex items-center gap-2">
                        <CreditCard size={16} />
                        CNIC: {user.cnic}
                      </div>
                      {user.role === 'DOCTOR' && (
                        <>
                          <div className="flex items-center gap-2">
                            <FileText size={16} />
                            License: {user.licenseNumber}
                          </div>
                          <div className="flex items-center gap-2">
                            <FileText size={16} />
                            Specialization: {user.specialization}
                          </div>
                        </>
                      )}
                      {user.role === 'PATIENT' && (
                        <>
                          <div className="flex items-center gap-2">
                            Age: {user.age} | Gender: {user.gender}
                          </div>
                        </>
                      )}
                    </div>
                    <p className="text-xs text-gray-500 mt-2">
                      Registered: {new Date(user.createdAt).toLocaleString()}
                    </p>
                  </div>
                </div>

                <div className="flex gap-2 flex-shrink-0">
                  <button
                    onClick={() => handleViewDetails(user.userId)}
                    className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 flex items-center gap-2"
                  >
                    <Eye size={18} />
                    Review
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow p-12 text-center">
          <CheckCircle size={48} className="mx-auto mb-4 text-gray-300" />
          <p className="text-gray-500 text-lg">No pending registrations</p>
          <p className="text-gray-400 text-sm mt-2">
            All registrations have been processed
          </p>
        </div>
      )}

      {/* Review Modal */}
      {showModal && selectedUser && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4 overflow-y-auto">
          <div className="bg-white rounded-lg max-w-3xl w-full p-6 max-h-screen overflow-y-auto">
            {/* Modal Header */}
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-2xl font-bold">Review Registration</h2>
              <button
                onClick={() => {
                  setShowModal(false);
                  setSelectedUser(null);
                  setRejectReason('');
                }}
                className="text-gray-500 hover:text-gray-700"
              >
                <XCircle size={24} />
              </button>
            </div>

            {/* User Details */}
            <div className="space-y-6">
              {/* Basic Info */}
              <div className="bg-gray-50 rounded-lg p-4">
                <h3 className="font-semibold text-gray-900 mb-3">Basic Information</h3>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <span className="text-gray-600">Full Name:</span>
                    <p className="font-medium">{selectedUser.fullName}</p>
                  </div>
                  <div>
                    <span className="text-gray-600">Email:</span>
                    <p className="font-medium">{selectedUser.email}</p>
                  </div>
                  <div>
                    <span className="text-gray-600">CNIC:</span>
                    <p className="font-medium">{selectedUser.cnic}</p>
                  </div>
                  <div>
                    <span className="text-gray-600">Role:</span>
                    <p className="font-medium">{selectedUser.role}</p>
                  </div>
                </div>
              </div>

              {/* Patient Specific */}
              {selectedUser.role === 'PATIENT' && (
                <div className="bg-blue-50 rounded-lg p-4">
                  <h3 className="font-semibold text-gray-900 mb-3">Patient Details</h3>
                  <div className="grid grid-cols-2 gap-4 text-sm">
                    <div>
                      <span className="text-gray-600">Age:</span>
                      <p className="font-medium">{selectedUser.age}</p>
                    </div>
                    <div>
                      <span className="text-gray-600">Gender:</span>
                      <p className="font-medium capitalize">{selectedUser.gender}</p>
                    </div>
                    {selectedUser.address && (
                      <div className="col-span-2">
                        <span className="text-gray-600">Address:</span>
                        <p className="font-medium">{selectedUser.address}</p>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Doctor Specific */}
              {selectedUser.role === 'DOCTOR' && (
                <>
                  <div className="bg-purple-50 rounded-lg p-4">
                    <h3 className="font-semibold text-gray-900 mb-3">Doctor Details</h3>
                    <div className="grid grid-cols-2 gap-4 text-sm">
                      <div>
                        <span className="text-gray-600">Specialization:</span>
                        <p className="font-medium">{selectedUser.specialization}</p>
                      </div>
                      <div>
                        <span className="text-gray-600">License Number:</span>
                        <p className="font-medium">{selectedUser.licenseNumber}</p>
                      </div>
                    </div>
                  </div>

                  {/* License Document */}
                  {selectedUser.licenseDocumentPath && (
                    <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                      <div className="flex items-center justify-between">
                        <div>
                          <h3 className="font-semibold text-gray-900 mb-1">License Document</h3>
                          <p className="text-sm text-gray-600">
                            Verify the medical license before approval
                          </p>
                        </div>

                        <a
                        
                          href={`/api/admin/registrations/${selectedUser.userId}/document`}
                          download
                          className="px-4 py-2 bg-yellow-600 text-white rounded-lg hover:bg-yellow-700 flex items-center gap-2"
                        >
                          <Download size={18} />
                          Download
                        </a>
                      </div>
                    </div>
                  )}
                </>
              )}

              {/* Rejection Reason Input (shown when rejecting) */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Rejection Reason (required if rejecting)
                </label>
                <textarea
                  value={rejectReason}
                  onChange={(e) => setRejectReason(e.target.value)}
                  rows={3}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="Provide a clear reason for rejection..."
                />
              </div>

              {/* Action Buttons */}
              <div className="flex gap-3 pt-4 border-t">
                <button
                  onClick={() => handleApprove(selectedUser.userId)}
                  disabled={actionLoading}
                  className="flex-1 bg-green-600 text-white py-3 rounded-lg hover:bg-green-700 disabled:bg-gray-400 flex items-center justify-center gap-2"
                >
                  {actionLoading ? (
                    <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                  ) : (
                    <>
                      <CheckCircle size={20} />
                      Approve Registration
                    </>
                  )}
                </button>
                <button
                  onClick={() => handleReject(selectedUser.userId)}
                  disabled={actionLoading || !rejectReason.trim()}
                  className="flex-1 bg-red-600 text-white py-3 rounded-lg hover:bg-red-700 disabled:bg-gray-400 flex items-center justify-center gap-2"
                >
                  <XCircle size={20} />
                  Reject Registration
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}