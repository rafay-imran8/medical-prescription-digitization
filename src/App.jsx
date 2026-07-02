
// src/App.jsx
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthProvider';
import MainLayout from './layouts/MainLayout';
// Auth Pages
import Login from './pages/auth/Login';
import PatientRegistration from './auth/PatientRegistration';
import DoctorRegistration from './auth/DoctorRegistration';
import RegistrationPending from './auth/RegistrationPending';
import SetPassword from './auth/SetPassword';

// Layouts
import AdminLayout from './layouts/AdminLayout';  // ← ADD THIS

// Patient Pages
import PatientDashboard from './pages/patient/PatientDashboard';
import PrescriptionDetail from './pages/patient/PrescriptionDetail';
import PatientAccess from './pages/patient/PatientAccess';

// Doctor Pages
import DoctorDashboard from './pages/doctor/DoctorDashboard';
import DoctorPatientSearch from './pages/doctor/DoctorPatientSearch';
import DoctorAccessCodeEntry from './pages/doctor/DoctorCodeAcessEntry';
import CreatePrescription from './pages/doctor/CreatePrescription';
import PatientPrescriptions from './pages/doctor/PatientPrescriptions';

// Admin Pages
import AdminDashboard from './pages/admin/AdminDashboard';
import AdminRegistrations from './pages/admin/AdminRegistrations';
import AdminFeedback from './pages/admin/AdminFeedback';
import AdminUserLogs from './pages/admin/AdminUserLogs';

// Analyst Pages
import AnalystDashboard from './pages/analyst/AnalystDashboard';

// Protected Route
import ProtectedRoute from './components/common/ProtectedRoute';

function App() {
  return (
    <AuthProvider>
      <Router>
        <Toaster position="top-right" />

        <Routes>
          {/* Public Routes */}
          <Route path="/" element={<Navigate to="/login" />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register/patient" element={<PatientRegistration />} />
          <Route path="/register/doctor" element={<DoctorRegistration />} />
          <Route path="/registration-pending" element={<RegistrationPending />} />
          <Route path="/set-password" element={<SetPassword />} />

         {/* Patient Routes */}
<Route
  path="/patient"
  element={
    <ProtectedRoute roles={['PATIENT']}>
      <MainLayout />
    </ProtectedRoute>
  }
>
  <Route path="dashboard" element={<PatientDashboard />} />
  <Route path="prescriptions/:id" element={<PrescriptionDetail />} />
  <Route path="access" element={<PatientAccess />} />
</Route>

{/* Doctor Routes */}
<Route
  path="/doctor"
  element={
    <ProtectedRoute roles={['DOCTOR']}>
      <MainLayout />
    </ProtectedRoute>
  }
>
  <Route path="dashboard" element={<DoctorDashboard />} />
  <Route path="patients/search" element={<DoctorPatientSearch />} />
  <Route path="access/activate" element={<DoctorAccessCodeEntry />} />
  <Route path="prescriptions/create" element={<CreatePrescription />} />
  <Route path="patients/:patientId/prescriptions" element={<PatientPrescriptions />} />
</Route>

{/* Analyst Routes */}
<Route
  path="/analyst"
  element={
    <ProtectedRoute roles={['ANALYST']}>
      <MainLayout />
    </ProtectedRoute>
  }
>
  <Route path="dashboard" element={<AnalystDashboard />} />
</Route>

          {/* Admin Routes - NOW WITH LAYOUT */}
          <Route
            path="/admin"
            element={
              <ProtectedRoute roles={['ADMIN']}>
                <AdminLayout />
              </ProtectedRoute>
            }
          >
            <Route path="dashboard" element={<AdminDashboard />} />
            <Route path="users" element={<AdminDashboard />} />        
            <Route path="registrations" element={<AdminRegistrations />} />
            <Route path="feedback" element={<AdminFeedback />} />
            <Route path="logs" element={<AdminUserLogs />} />         
          </Route>

        
          {/* 404 */}
          <Route path="*" element={<Navigate to="/login" />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;