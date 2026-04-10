// src/services/api.js
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

// Create axios instance
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add token to requests
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Handle token expiration
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// ==================== AUTH API ====================
export const authAPI = {
  // Patient Registration
  registerPatient: (data) =>
    api.post('/auth/register/patient', data),

  // Doctor Registration (FormData for file upload)
  registerDoctor: (formData) =>
    api.post('/auth/register/doctor', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }),

  // Validate Token
  validateToken: (token) =>
    api.get('/auth/validate-token', { params: { token } }),

  // Set Password
  setPassword: (data) =>
    api.post('/auth/set-password', data),

  // Login
  login: (credentials) =>
    api.post('/auth/login', credentials),

  // Logout
  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },
};

// ==================== ADMIN API ====================
export const adminAPI = {
  // User Management
  getAllUsers: () =>
    api.get('/admin/users'),

  getUserLogs: () =>
  api.get('/admin/users/logs'),

  updateUserStatus: (userId, isActive) =>
    api.put(`/admin/users/${userId}/status`, { isActive }),

  // Registration Approval
  getPendingRegistrations: () =>
    api.get('/admin/registrations/pending'),

  getUserDetails: (userId) =>
    api.get(`/admin/registrations/${userId}`),

  approveRegistration: (data) =>
    api.post('/admin/registrations/approve', data),

  rejectRegistration: (data) =>
    api.post('/admin/registrations/reject', data),

  downloadDocument: (userId) =>
    api.get(`/admin/registrations/${userId}/document`, {
      responseType: 'blob'
    }),

  // Feedback
  getAllFeedbacks: () =>
    api.get('/feedback/all'),

  updateFeedbackStatus: (feedbackId, status) =>
    api.put(`/feedback/${feedbackId}/status`, null, { params: { status } }),

  // Create Admin/Analyst (admin only)
  createAdminUser: (data) =>
    api.post('/admin/users/create', data),
};

// ==================== PATIENT API ====================
export const patientAPI = {
  getProfile: () =>
    api.get('/patient/profile'),

  getPrescriptions: () =>
    api.get('/patient/prescriptions'),

  getPrescriptionById: (id) =>
    api.get(`/patient/prescriptions/${id}`),

  // getPrescriptionImage: (id) =>
  //   api.get(`/patient/prescriptions/${id}/image`, { responseType: 'blob' }),
  getPrescriptionImage: (id) => {
  const token = localStorage.getItem('token');
  return axios.get(`${API_BASE_URL}/patient/prescriptions/${id}/image`, {
    responseType: 'blob',
    headers: {
      Authorization: `Bearer ${token}`
    }
  });
},

  uploadPrescription: (formData, config) =>
    api.post('/patient/prescriptions/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      ...config
    }),

  // Access Management
  getPendingAccessRequests: () =>
    api.get('/patient/access/pending'),

  getGrantedDoctors: () =>
    api.get('/patient/access/granted'),

  grantAccessWithCode: (doctorId) =>
    api.post(`/patient/access/grant/${doctorId}`),

  revokeAccess: (doctorId) =>
    api.delete(`/patient/access/revoke/${doctorId}`),

  enterAccessCode: (accessCode) =>
    api.post('/patient/access/enter-code', { accessCode }),
};

// ==================== DOCTOR API ====================
export const doctorAPI = {
  getProfile: () =>
    api.get('/doctor/profile'),

  getMyPatients: () =>
    api.get('/doctor/patients'),

  searchPatients: (patientId) =>
    api.get('/doctor/patients/search', { params: { patientId } }),

  requestAccess: (patientId) =>
    api.post(`/doctor/access/request/${patientId}`),

  activateAccess: (accessCode) =>
    api.post('/doctor/access/activate', { accessCode }),

  getPatientPrescriptions: (patientId) =>
    api.get(`/doctor/patients/${patientId}/prescriptions`),

  createPrescription: (data) =>
    api.post('/doctor/prescriptions/create', data),
};

// ==================== FEEDBACK API ====================
export const feedbackAPI = {
  submitFeedback: (data) =>
    api.post('/feedback', data),

  getMyFeedbacks: () =>
    api.get('/feedback/my'),
    
};

export default api;