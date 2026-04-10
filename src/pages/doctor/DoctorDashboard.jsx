// src/pages/doctor/DoctorDashboard.jsx
import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { 
  Users, FileText, Search, Key, PlusCircle,
  Activity, Clock
} from 'lucide-react';
import { doctorAPI } from '../../services/api';

export default function DoctorDashboard() {
  const [stats, setStats] = useState({
    totalPatients: 0,
    activePrescriptions: 0,
    pendingRequests: 0
  });

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      const patientsRes = await doctorAPI.getMyPatients();
      setStats({
        totalPatients: patientsRes.data.length,
        activePrescriptions: 0, // Calculate from prescriptions
        pendingRequests: 0
      });
    } catch (error) {
      console.error('Error fetching dashboard data:', error);
    }
  };

  return (
    <div className="space-y-6">
      {/* Welcome Header */}
      <div className="bg-white rounded-lg shadow p-6">
        <h1 className="text-2xl font-bold mb-2">Doctor Dashboard</h1>
        <p className="text-gray-600">
          Manage your patients and prescriptions
        </p>
      </div>

      {/* Quick Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-gray-600 text-sm">Total Patients</p>
              <p className="text-3xl font-bold text-blue-600">{stats.totalPatients}</p>
            </div>
            <Users size={40} className="text-blue-600" />
          </div>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-gray-600 text-sm">Active Prescriptions</p>
              <p className="text-3xl font-bold text-green-600">{stats.activePrescriptions}</p>
            </div>
            <FileText size={40} className="text-green-600" />
          </div>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-gray-600 text-sm">Pending Access</p>
              <p className="text-3xl font-bold text-yellow-600">{stats.pendingRequests}</p>
            </div>
            <Clock size={40} className="text-yellow-600" />
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-xl font-semibold mb-4">Quick Actions</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <Link
            to="/doctor/patients/search"
            className="flex items-center gap-3 p-4 border border-gray-200 rounded-lg hover:shadow-md hover:border-blue-500 transition-all group"
          >
            <Search size={24} className="text-blue-600 group-hover:scale-110 transition-transform" />
            <div>
              <p className="font-semibold text-gray-900">Search Patients</p>
              <p className="text-xs text-gray-500">Find and request access</p>
            </div>
          </Link>

          <Link
            to="/doctor/access/activate"
            className="flex items-center gap-3 p-4 border border-gray-200 rounded-lg hover:shadow-md hover:border-green-500 transition-all group"
          >
            <Key size={24} className="text-green-600 group-hover:scale-110 transition-transform" />
            <div>
              <p className="font-semibold text-gray-900">Enter Access Code</p>
              <p className="text-xs text-gray-500">Activate patient access</p>
            </div>
          </Link>

          <Link
            to="/doctor/prescriptions/create"
            className="flex items-center gap-3 p-4 border border-gray-200 rounded-lg hover:shadow-md hover:border-purple-500 transition-all group"
          >
            <PlusCircle size={24} className="text-purple-600 group-hover:scale-110 transition-transform" />
            <div>
              <p className="font-semibold text-gray-900">Create Prescription</p>
              <p className="text-xs text-gray-500">Write digital Rx</p>
            </div>
          </Link>

          <Link
            to="/doctor/patients"
            className="flex items-center gap-3 p-4 border border-gray-200 rounded-lg hover:shadow-md hover:border-indigo-500 transition-all group"
          >
            <Users size={24} className="text-indigo-600 group-hover:scale-110 transition-transform" />
            <div>
              <p className="font-semibold text-gray-900">My Patients</p>
              <p className="text-xs text-gray-500">View patient list</p>
            </div>
          </Link>
        </div>
      </div>
    </div>
  );
}