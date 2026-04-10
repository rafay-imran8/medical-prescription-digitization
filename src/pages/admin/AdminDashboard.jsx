import { useState, useEffect } from 'react';
import { adminAPI } from '../../services/api';
import { 
  Users, UserCheck, UserX, Shield, 
  CheckCircle, XCircle
} from 'lucide-react';
import toast from 'react-hot-toast';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import Modal from '../../components/common/Modal';
import { formatDate, getRoleDisplay } from '../../utils/helpers';

const AdminDashboard = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterRole, setFilterRole] = useState('ALL');
  const [statusModal, setStatusModal] = useState({ open: false, user: null });
  const [verifyModal, setVerifyModal] = useState({ open: false, user: null });

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      const response = await adminAPI.getAllUsers();
      setUsers(response.data.data);
    } catch (error) {
      toast.error('Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleStatus = async (userId, currentStatus) => {
    try {
      await adminAPI.updateUserStatus(userId, !currentStatus);
      toast.success(`User ${!currentStatus ? 'activated' : 'deactivated'} successfully`);
      fetchUsers();
      setStatusModal({ open: false, user: null });
    } catch (error) {
      toast.error('Failed to update user status');
    }
  };

  const handleVerifyUser = async (userId) => {
    try {
      await adminAPI.verifyUser(userId);
      toast.success('User verified successfully');
      fetchUsers();
      setVerifyModal({ open: false, user: null });
    } catch (error) {
      toast.error('Failed to verify user');
    }
  };

  const filteredUsers = filterRole === 'ALL' 
    ? users 
    : users.filter(u => u.role === filterRole);

  const stats = {
    total: users.length,
    patients: users.filter(u => u.role === 'PATIENT').length,
    doctors: users.filter(u => u.role === 'DOCTOR').length,
    active: users.filter(u => u.isActive).length,
    pending: users.filter(u => !u.isVerified && u.role === 'DOCTOR').length,
  };

  if (loading) {
    return <LoadingSpinner fullScreen />;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">User Management</h1>
          <p className="text-gray-600 mt-2">
            Manage users, verify doctors, and control access
          </p>
        </div>

        {/* Stats */}
        <div className="grid md:grid-cols-5 gap-4 mb-8">
          <div className="card">
            <div className="flex items-center space-x-3">
              <Users className="h-8 w-8 text-blue-600" />
              <div>
                <p className="text-sm text-gray-600">Total Users</p>
                <p className="text-2xl font-bold">{stats.total}</p>
              </div>
            </div>
          </div>

          <div className="card">
            <div className="flex items-center space-x-3">
              <UserCheck className="h-8 w-8 text-green-600" />
              <div>
                <p className="text-sm text-gray-600">Active</p>
                <p className="text-2xl font-bold">{stats.active}</p>
              </div>
            </div>
          </div>

          <div className="card">
            <div className="flex items-center space-x-3">
              <Shield className="h-8 w-8 text-purple-600" />
              <div>
                <p className="text-sm text-gray-600">Patients</p>
                <p className="text-2xl font-bold">{stats.patients}</p>
              </div>
            </div>
          </div>

          <div className="card">
            <div className="flex items-center space-x-3">
              <Shield className="h-8 w-8 text-orange-600" />
              <div>
                <p className="text-sm text-gray-600">Doctors</p>
                <p className="text-2xl font-bold">{stats.doctors}</p>
              </div>
            </div>
          </div>

          <div className="card">
            <div className="flex items-center space-x-3">
              <UserX className="h-8 w-8 text-yellow-600" />
              <div>
                <p className="text-sm text-gray-600">Pending</p>
                <p className="text-2xl font-bold">{stats.pending}</p>
              </div>
            </div>
          </div>
        </div>

        {/* Filters */}
        <div className="card mb-6">
          <div className="flex items-center space-x-4">
            <span className="text-sm font-medium text-gray-700">Filter by role:</span>
            {['ALL', 'PATIENT', 'DOCTOR', 'ADMIN', 'ANALYST'].map((role) => (
              <button
                key={role}
                onClick={() => setFilterRole(role)}
                className={`px-4 py-2 rounded-lg transition-colors ${
                  filterRole === role
                    ? 'bg-primary-600 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                {role}
              </button>
            ))}
          </div>
        </div>

        {/* Users Table */}
        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    User
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Role
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Verified
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Joined
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {filteredUsers.map((user) => (
                  <tr key={user.userId} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div>
                        <p className="font-medium text-gray-900">{user.fullName}</p>
                        <p className="text-sm text-gray-500">{user.email}</p>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="badge badge-info">
                        {getRoleDisplay(user.role)}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {user.isActive ? (
                        <span className="badge badge-success flex items-center space-x-1 w-fit">
                          <CheckCircle className="h-3 w-3" />
                          <span>Active</span>
                        </span>
                      ) : (
                        <span className="badge badge-danger flex items-center space-x-1 w-fit">
                          <XCircle className="h-3 w-3" />
                          <span>Inactive</span>
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {user.isVerified ? (
                        <CheckCircle className="h-5 w-5 text-green-600" />
                      ) : (
                        <XCircle className="h-5 w-5 text-red-600" />
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {formatDate(user.createdAt)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm space-x-2">
                      <button
                        onClick={() => setStatusModal({ open: true, user })}
                        className={`px-3 py-1 rounded ${
                          user.isActive
                            ? 'bg-red-100 text-red-700 hover:bg-red-200'
                            : 'bg-green-100 text-green-700 hover:bg-green-200'
                        }`}
                      >
                        {user.isActive ? 'Deactivate' : 'Activate'}
                      </button>
                      
                      {!user.isVerified && user.role === 'DOCTOR' && (
                        <button
                          onClick={() => setVerifyModal({ open: true, user })}
                          className="px-3 py-1 rounded bg-blue-100 text-blue-700 hover:bg-blue-200"
                        >
                          Verify
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Status Toggle Modal */}
      <Modal
        isOpen={statusModal.open}
        onClose={() => setStatusModal({ open: false, user: null })}
        title="Change User Status"
        footer={
          <>
            <button
              onClick={() => setStatusModal({ open: false, user: null })}
              className="btn-secondary"
            >
              Cancel
            </button>
            <button
              onClick={() => handleToggleStatus(statusModal.user?.userId, statusModal.user?.isActive)}
              className={statusModal.user?.isActive ? 'btn-danger' : 'btn-primary'}
            >
              {statusModal.user?.isActive ? 'Deactivate' : 'Activate'}
            </button>
          </>
        }
      >
        <p className="text-gray-700">
          Are you sure you want to{' '}
          {statusModal.user?.isActive ? 'deactivate' : 'activate'}{' '}
          <strong>{statusModal.user?.fullName}</strong>?
        </p>
      </Modal>

      {/* Verify User Modal */}
      <Modal
        isOpen={verifyModal.open}
        onClose={() => setVerifyModal({ open: false, user: null })}
        title="Verify Doctor"
        footer={
          <>
            <button
              onClick={() => setVerifyModal({ open: false, user: null })}
              className="btn-secondary"
            >
              Cancel
            </button>
            <button
              onClick={() => handleVerifyUser(verifyModal.user?.userId)}
              className="btn-primary"
            >
              Verify
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <p className="text-gray-700">
            Verify <strong>{verifyModal.user?.fullName}</strong> as a doctor?
          </p>
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p className="text-sm text-blue-900">
              ℹ️ Verification allows the doctor to fully use the system and request patient access.
            </p>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default AdminDashboard;