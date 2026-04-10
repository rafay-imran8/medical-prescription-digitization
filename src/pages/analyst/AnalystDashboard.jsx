import { useState, useEffect } from 'react';
import { 
  TrendingUp, Activity, Pill, AlertCircle,
  Calendar, BarChart3
} from 'lucide-react';
import axios from 'axios';
import toast from 'react-hot-toast';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { formatDate } from '../../utils/helpers';

const AnalystDashboard = () => {
  const [statistics, setStatistics] = useState(null);
  const [trends, setTrends] = useState(null);
  const [medicines, setMedicines] = useState(null);
  const [diagnoses, setDiagnoses] = useState(null);
  const [loading, setLoading] = useState(true);
  const [dateRange, setDateRange] = useState({
    startDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    endDate: new Date().toISOString().split('T')[0]
  });

  useEffect(() => {
    fetchAllData();
  }, []);

  const fetchAllData = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      const config = { headers: { Authorization: `Bearer ${token}` } };

      const [statsRes, trendsRes, medsRes, diagRes] = await Promise.all([
        axios.get('http://localhost:8080/api/analyst/statistics', config),
        axios.get(`http://localhost:8080/api/analyst/trends?startDate=${dateRange.startDate}&endDate=${dateRange.endDate}`, config),
        axios.get(`http://localhost:8080/api/analyst/medicines?startDate=${dateRange.startDate}&endDate=${dateRange.endDate}`, config),
        axios.get(`http://localhost:8080/api/analyst/diagnoses?startDate=${dateRange.startDate}&endDate=${dateRange.endDate}`, config)
      ]);

      setStatistics(statsRes.data.data);
      setTrends(trendsRes.data.data);
      setMedicines(medsRes.data.data);
      setDiagnoses(diagRes.data.data);
    } catch (error) {
      toast.error('Failed to load analytics');
    } finally {
      setLoading(false);
    }
  };

  const handleDateRangeChange = (e) => {
    setDateRange({
      ...dateRange,
      [e.target.name]: e.target.value
    });
  };

  const applyFilter = () => {
    fetchAllData();
  };

  if (loading) {
    return <LoadingSpinner fullScreen />;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Analytics Dashboard</h1>
          <p className="text-gray-600 mt-2">
            Prescription trends and insights
          </p>
        </div>

        {/* Date Range Filter */}
        <div className="card mb-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Date Range Filter</h2>
          <div className="grid md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Start Date
              </label>
              <input
                type="date"
                name="startDate"
                value={dateRange.startDate}
                onChange={handleDateRangeChange}
                className="input-field"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                End Date
              </label>
              <input
                type="date"
                name="endDate"
                value={dateRange.endDate}
                onChange={handleDateRangeChange}
                className="input-field"
              />
            </div>
            <div className="flex items-end">
              <button onClick={applyFilter} className="btn-primary w-full">
                Apply Filter
              </button>
            </div>
          </div>
        </div>

        {/* Statistics Cards */}
        {statistics && (
          <div className="grid md:grid-cols-4 gap-6 mb-8">
            <div className="card">
              <div className="flex items-center space-x-4">
                <div className="bg-blue-100 p-3 rounded-lg">
                  <BarChart3 className="h-8 w-8 text-blue-600" />
                </div>
                <div>
                  <p className="text-sm text-gray-600">Total Prescriptions</p>
                  <p className="text-2xl font-bold">{statistics.total_prescriptions}</p>
                </div>
              </div>
            </div>

            <div className="card">
              <div className="flex items-center space-x-4">
                <div className="bg-green-100 p-3 rounded-lg">
                  <TrendingUp className="h-8 w-8 text-green-600" />
                </div>
                <div>
                  <p className="text-sm text-gray-600">Completed</p>
                  <p className="text-2xl font-bold">{statistics.completed}</p>
                </div>
              </div>
            </div>

            <div className="card">
              <div className="flex items-center space-x-4">
                <div className="bg-yellow-100 p-3 rounded-lg">
                  <Activity className="h-8 w-8 text-yellow-600" />
                </div>
                <div>
                  <p className="text-sm text-gray-600">Processing</p>
                  <p className="text-2xl font-bold">{statistics.processing}</p>
                </div>
              </div>
            </div>

            <div className="card">
              <div className="flex items-center space-x-4">
                <div className="bg-purple-100 p-3 rounded-lg">
                  <AlertCircle className="h-8 w-8 text-purple-600" />
                </div>
                <div>
                  <p className="text-sm text-gray-600">Success Rate</p>
                  <p className="text-2xl font-bold">{statistics.success_rate.toFixed(1)}%</p>
                </div>
              </div>
            </div>
          </div>
        )}

        <div className="grid lg:grid-cols-2 gap-6">
          {/* Top Medicines */}
          {medicines && (
            <div className="card">
              <h2 className="text-xl font-semibold text-gray-900 mb-6 flex items-center space-x-2">
                <Pill className="h-5 w-5" />
                <span>Top Prescribed Medicines</span>
              </h2>
              <div className="space-y-3">
                {medicines.top_medicines.slice(0, 10).map((med, index) => (
                  <div key={index} className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      <span className="text-sm font-medium text-gray-500">
                        #{index + 1}
                      </span>
                      <span className="text-gray-900">{med.medicine}</span>
                    </div>
                    <span className="badge badge-info">{med.count} times</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Top Diagnoses */}
          {diagnoses && (
            <div className="card">
              <h2 className="text-xl font-semibold text-gray-900 mb-6 flex items-center space-x-2">
                <AlertCircle className="h-5 w-5" />
                <span>Common Diagnoses</span>
              </h2>
              <div className="space-y-3">
                {diagnoses.top_diagnoses.slice(0, 10).map((diag, index) => (
                  <div key={index} className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      <span className="text-sm font-medium text-gray-500">
                        #{index + 1}
                      </span>
                      <span className="text-gray-900">{diag.diagnosis}</span>
                    </div>
                    <span className="badge badge-warning">{diag.count} cases</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default AnalystDashboard;