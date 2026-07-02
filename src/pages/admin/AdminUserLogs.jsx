// src/pages/admin/AdminUserLogs.jsx
import { useState, useEffect } from 'react';
import {
  Activity, User, Mail, Clock, Shield,
  CheckCircle, XCircle, AlertCircle, Search
} from 'lucide-react';
import { adminAPI } from '../../services/api';
import toast from 'react-hot-toast';

const getRoleBadge = (role) => {
  const colors = {
    PATIENT:  'bg-blue-100 text-blue-800',
    DOCTOR:   'bg-purple-100 text-purple-800',
    ADMIN:    'bg-red-100 text-red-800',
    ANALYST:  'bg-orange-100 text-orange-800',
  };
  return colors[role] || 'bg-gray-100 text-gray-800';
};

const getStatusBadge = (status) => {
  const colors = {
    APPROVED: 'bg-green-100 text-green-800',
    PENDING:  'bg-yellow-100 text-yellow-800',
    REJECTED: 'bg-red-100 text-red-800',
  };
  return colors[status] || 'bg-gray-100 text-gray-800';
};

const formatDateTime = (dt) => {
  if (!dt) return <span className="text-gray-400 italic">Never</span>;
  const d = new Date(dt);
  return (
    <span title={d.toISOString()}>
      {d.toLocaleDateString()} {d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
    </span>
  );
};

export default function AdminUserLogs() {
  const [logs, setLogs]         = useState([]);
  const [loading, setLoading]   = useState(true);
  const [search, setSearch]     = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [expanded, setExpanded] = useState(null);   // userId of expanded row

  useEffect(() => {
    fetchLogs();
  }, []);

  const fetchLogs = async () => {
    try {
      const response = await adminAPI.getUserLogs();
      setLogs(response.data.data);
    } catch (error) {
      toast.error('Failed to load user logs');
    } finally {
      setLoading(false);
    }
  };

  const filtered = logs.filter((u) => {
    const matchRole   = roleFilter === 'ALL' || u.role === roleFilter;
    const matchSearch = !search ||
      u.fullName.toLowerCase().includes(search.toLowerCase()) ||
      u.email.toLowerCase().includes(search.toLowerCase());
    return matchRole && matchSearch;
  });

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">

        {/* Header */}
        <div>
          <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-3">
            <Activity className="h-8 w-8 text-blue-600" />
            User Activity Logs
          </h1>
          <p className="text-gray-600 mt-1">
            Last login times, account status, and admin notes for all users
          </p>
        </div>

        {/* Summary Cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[
            { label: 'Total Users',  value: logs.length,                                     color: 'blue'   },
            { label: 'Active',       value: logs.filter(u => u.isActive).length,              color: 'green'  },
            { label: 'Never Logged', value: logs.filter(u => !u.lastLogin).length,            color: 'yellow' },
            { label: 'Rejected',     value: logs.filter(u => u.accountStatus === 'REJECTED').length, color: 'red' },
          ].map(({ label, value, color }) => (
            <div key={label} className="bg-white rounded-lg shadow p-4 flex items-center gap-3">
              <div className={`h-10 w-10 rounded-full bg-${color}-100 flex items-center justify-center`}>
                <span className={`text-${color}-700 font-bold text-lg`}>{value}</span>
              </div>
              <span className="text-sm text-gray-600">{label}</span>
            </div>
          ))}
        </div>

        {/* Filters */}
        <div className="bg-white rounded-lg shadow p-4 flex flex-col sm:flex-row gap-4">
          {/* Search */}
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
            <input
              type="text"
              placeholder="Search by name or email..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-9 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
            />
          </div>
          {/* Role filter */}
          <div className="flex gap-2 flex-wrap">
            {['ALL', 'PATIENT', 'DOCTOR', 'ADMIN', 'ANALYST'].map((r) => (
              <button
                key={r}
                onClick={() => setRoleFilter(r)}
                className={`px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                  roleFilter === r
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                {r}
              </button>
            ))}
          </div>
        </div>

        {/* Table */}
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  {['User', 'Role', 'Account Status', 'Active', 'Verified',
                    'Last Login', 'Joined', 'Details'].map((h) => (
                    <th
                      key={h}
                      className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                    >
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {filtered.map((u) => (
                  <>
                    <tr
                      key={u.userId}
                      className="hover:bg-gray-50 cursor-pointer"
                      onClick={() => setExpanded(expanded === u.userId ? null : u.userId)}
                    >
                      {/* User */}
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <div className="h-8 w-8 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0">
                            <User className="h-4 w-4 text-blue-700" />
                          </div>
                          <div>
                            <p className="font-medium text-gray-900">{u.fullName}</p>
                            <p className="text-xs text-gray-500 flex items-center gap-1">
                              <Mail className="h-3 w-3" /> {u.email}
                            </p>
                          </div>
                        </div>
                      </td>

                      {/* Role */}
                      <td className="px-4 py-3">
                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${getRoleBadge(u.role)}`}>
                          {u.role}
                        </span>
                      </td>

                      {/* Account Status */}
                      <td className="px-4 py-3">
                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusBadge(u.accountStatus)}`}>
                          {u.accountStatus}
                        </span>
                      </td>

                      {/* Active */}
                      <td className="px-4 py-3">
                        {u.isActive
                          ? <CheckCircle className="h-5 w-5 text-green-500" />
                          : <XCircle    className="h-5 w-5 text-red-500"   />}
                      </td>

                      {/* Verified */}
                      <td className="px-4 py-3">
                        {u.isVerified
                          ? <CheckCircle className="h-5 w-5 text-green-500" />
                          : <XCircle    className="h-5 w-5 text-red-500"   />}
                      </td>

                      {/* Last Login */}
                      <td className="px-4 py-3 text-gray-600">
                        <div className="flex items-center gap-1">
                          <Clock className="h-3 w-3 text-gray-400 flex-shrink-0" />
                          {formatDateTime(u.lastLogin)}
                        </div>
                      </td>

                      {/* Joined */}
                      <td className="px-4 py-3 text-gray-600">
                        {formatDateTime(u.createdAt)}
                      </td>

                      {/* Expand toggle */}
                      <td className="px-4 py-3">
                        <button className="text-blue-600 hover:text-blue-800 text-xs font-medium">
                          {expanded === u.userId ? 'Hide ▲' : 'Show ▼'}
                        </button>
                      </td>
                    </tr>

                    {/* Expanded detail row */}
                    {expanded === u.userId && (
                      <tr key={`${u.userId}-detail`} className="bg-blue-50">
                        <td colSpan={8} className="px-6 py-4">
                          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">

                            <div className="space-y-2">
                              <h4 className="font-semibold text-gray-700 flex items-center gap-1">
                                <Shield className="h-4 w-4" /> Identity
                              </h4>
                              <p><span className="text-gray-500">CNIC:</span>{' '}
                                <span className="font-medium">{u.cnic || '—'}</span>
                              </p>
                              <p><span className="text-gray-500">User ID:</span>{' '}
                                <span className="font-mono text-xs">{u.userId}</span>
                              </p>
                            </div>

                            <div className="space-y-2">
                              <h4 className="font-semibold text-gray-700 flex items-center gap-1">
                                <Clock className="h-4 w-4" /> Timestamps
                              </h4>
                              <p><span className="text-gray-500">Last Updated:</span>{' '}
                                {formatDateTime(u.updatedAt)}
                              </p>
                              <p><span className="text-gray-500">Last Login:</span>{' '}
                                {formatDateTime(u.lastLogin)}
                              </p>
                            </div>

                            <div className="space-y-2">
                              <h4 className="font-semibold text-gray-700 flex items-center gap-1">
                                <AlertCircle className="h-4 w-4" /> Admin Info
                              </h4>
                              {u.rejectionReason && (
                                <p><span className="text-red-600 font-medium">Rejection:</span>{' '}
                                  {u.rejectionReason}
                                </p>
                              )}
                              {u.adminNotes && (
                                <p><span className="text-gray-500">Notes:</span>{' '}
                                  {u.adminNotes}
                                </p>
                              )}
                              {!u.rejectionReason && !u.adminNotes && (
                                <p className="text-gray-400 italic">No admin notes</p>
                              )}
                            </div>

                          </div>
                        </td>
                      </tr>
                    )}
                  </>
                ))}
              </tbody>
            </table>
          </div>

          {filtered.length === 0 && (
            <div className="text-center py-12 text-gray-500">
              No users match the current filter
            </div>
          )}
        </div>

      </div>
    </div>
  );
}