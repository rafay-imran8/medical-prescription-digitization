import { format, formatDistanceToNow, parseISO } from 'date-fns';

// ========================================
// DATE FORMATTING
// ========================================
export const formatDate = (date) => {
  if (!date) return 'N/A';
  try {
    const dateObj = typeof date === 'string' ? parseISO(date) : date;
    return format(dateObj, 'MMM dd, yyyy');
  } catch {
    return 'Invalid date';
  }
};

export const formatDateTime = (date) => {
  if (!date) return 'N/A';
  try {
    const dateObj = typeof date === 'string' ? parseISO(date) : date;
    return format(dateObj, 'MMM dd, yyyy hh:mm a');
  } catch {
    return 'Invalid date';
  }
};

export const formatRelativeTime = (date) => {
  if (!date) return 'N/A';
  try {
    const dateObj = typeof date === 'string' ? parseISO(date) : date;
    return formatDistanceToNow(dateObj, { addSuffix: true });
  } catch {
    return 'Invalid date';
  }
};

// ========================================
// FILE SIZE FORMATTING
// ========================================
export const formatFileSize = (bytes) => {
  if (!bytes) return 'N/A';
  const kb = bytes / 1024;
  const mb = kb / 1024;
  
  if (mb >= 1) return `${mb.toFixed(2)} MB`;
  if (kb >= 1) return `${kb.toFixed(2)} KB`;
  return `${bytes} bytes`;
};

// ========================================
// STATUS BADGE COLORS
// ========================================
export const getStatusColor = (status) => {
  const statusLower = status?.toLowerCase();
  
  switch (statusLower) {
    case 'completed':
    case 'active':
    case 'granted':
      return 'badge-success';
    case 'processing':
    case 'pending':
      return 'badge-warning';
    case 'failed':
    case 'revoked':
    case 'inactive':
      return 'badge-danger';
    default:
      return 'badge-info';
  }
};

// ========================================
// SEVERITY COLORS
// ========================================
export const getSeverityColor = (severity) => {
  const severityLower = severity?.toLowerCase();
  
  switch (severityLower) {
    case 'high':
      return 'text-red-600 bg-red-50';
    case 'medium':
      return 'text-yellow-600 bg-yellow-50';
    case 'low':
      return 'text-green-600 bg-green-50';
    default:
      return 'text-gray-600 bg-gray-50';
  }
};

// ========================================
// VALIDATION
// ========================================
export const validateEmail = (email) => {
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return re.test(email);
};

export const validatePassword = (password) => {
  // At least 8 chars, 1 uppercase, 1 lowercase, 1 number, 1 special char
  const re = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
  return re.test(password);
};

export const getPasswordStrength = (password) => {
  if (!password) return { strength: 0, label: 'None', color: 'gray' };
  
  let strength = 0;
  if (password.length >= 8) strength++;
  if (/[a-z]/.test(password)) strength++;
  if (/[A-Z]/.test(password)) strength++;
  if (/\d/.test(password)) strength++;
  if (/[@$!%*?&]/.test(password)) strength++;
  
  if (strength <= 2) return { strength, label: 'Weak', color: 'red' };
  if (strength <= 3) return { strength, label: 'Medium', color: 'yellow' };
  return { strength, label: 'Strong', color: 'green' };
};

// ========================================
// IMAGE VALIDATION
// ========================================
export const validateImageFile = (file) => {
  const validTypes = ['image/jpeg', 'image/jpg', 'image/png'];
  const maxSize = 10 * 1024 * 1024; // 10MB
  
  if (!validTypes.includes(file.type)) {
    return { valid: false, error: 'Please upload a JPG or PNG image' };
  }
  
  if (file.size > maxSize) {
    return { valid: false, error: 'Image must be less than 10MB' };
  }
  
  return { valid: true };
};

// ========================================
// ERROR HANDLING
// ========================================
export const getErrorMessage = (error) => {
  if (error.response?.data?.message) {
    return error.response.data.message;
  }
  if (error.message) {
    return error.message;
  }
  return 'An unexpected error occurred';
};

// ========================================
// ROLE DISPLAY
// ========================================
export const getRoleDisplay = (role) => {
  const roleMap = {
    PATIENT: 'Patient',
    DOCTOR: 'Doctor',
    ADMIN: 'Administrator',
    ANALYST: 'Analyst',
  };
  return roleMap[role] || role;
};

// ========================================
// COPY TO CLIPBOARD
// ========================================
export const copyToClipboard = async (text) => {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    return false;
  }
};