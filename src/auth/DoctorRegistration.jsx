// src/pages/auth/DoctorRegistration.jsx
import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { User, Mail, CreditCard, Phone, FileText, Upload, Stethoscope } from 'lucide-react';
import { authAPI } from '/src/services/api.js';
import toast from 'react-hot-toast';

export default function DoctorRegistration() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [licenseFile, setLicenseFile] = useState(null);
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    cnic: '',
    specialization: '',
    licenseNumber: '',
    contactNumber: ''
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      // Validate file type
      const validTypes = ['application/pdf', 'image/jpeg', 'image/png', 'image/jpg'];
      if (!validTypes.includes(file.type)) {
        toast.error('Please upload a PDF or image file');
        return;
      }

      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        toast.error('File size must be less than 5MB');
        return;
      }

      setLicenseFile(file);
    }
  };

  const validateCNIC = (cnic) => {
    const cnicRegex = /^\d{5}-\d{7}-\d{1}$/;
    return cnicRegex.test(cnic);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Validation
    if (!validateCNIC(formData.cnic)) {
      toast.error('Invalid CNIC format. Use: XXXXX-XXXXXXX-X');
      return;
    }

    if (!licenseFile) {
      toast.error('Please upload your medical license document');
      return;
    }

    setLoading(true);
    try {
      // Create FormData for file upload
      const formDataToSend = new FormData();
      formDataToSend.append('fullName', formData.fullName);
      formDataToSend.append('email', formData.email);
      formDataToSend.append('cnic', formData.cnic);
      formDataToSend.append('specialization', formData.specialization);
      formDataToSend.append('licenseNumber', formData.licenseNumber);
      formDataToSend.append('contactNumber', formData.contactNumber);
      formDataToSend.append('licenseDocument', licenseFile);

      const response = await authAPI.registerDoctor(formDataToSend);
      toast.success(response.data.message || 'Registration successful!');
      
      setTimeout(() => {
        navigate('/registration-pending');
      }, 2000);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-2xl w-full space-y-8">
        {/* Header */}
        <div className="text-center">
          <div className="flex items-center justify-center mb-4">
            <Stethoscope size={48} className="text-blue-600" />
          </div>
          <h2 className="text-3xl font-bold text-gray-900">Doctor Registration</h2>
          <p className="mt-2 text-sm text-gray-600">
            Register to prescribe and manage patient records
          </p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="bg-white shadow-lg rounded-lg p-8 space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Full Name */}
            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                <User className="inline mr-1" size={16} />
                Full Name *
              </label>
              <input
                type="text"
                name="fullName"
                value={formData.fullName}
                onChange={handleChange}
                required
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Dr. John Smith"
              />
            </div>

            {/* Email */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                <Mail className="inline mr-1" size={16} />
                Email Address *
              </label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                required
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="doctor@example.com"
              />
            </div>

            {/* CNIC */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                <CreditCard className="inline mr-1" size={16} />
                CNIC Number *
              </label>
              <input
                type="text"
                name="cnic"
                value={formData.cnic}
                onChange={handleChange}
                required
                pattern="\d{5}-\d{7}-\d{1}"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="XXXXX-XXXXXXX-X"
              />
            </div>

            {/* Specialization */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                <Stethoscope className="inline mr-1" size={16} />
                Specialization *
              </label>
              <input
                type="text"
                name="specialization"
                value={formData.specialization}
                onChange={handleChange}
                required
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="e.g., Cardiology, Pediatrics"
              />
            </div>

            {/* License Number */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                <FileText className="inline mr-1" size={16} />
                Medical License Number *
              </label>
              <input
                type="text"
                name="licenseNumber"
                value={formData.licenseNumber}
                onChange={handleChange}
                required
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="PMC-12345"
              />
            </div>

            {/* Contact Number */}
            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                <Phone className="inline mr-1" size={16} />
                Contact Number *
              </label>
              <input
                type="tel"
                name="contactNumber"
                value={formData.contactNumber}
                onChange={handleChange}
                required
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="+92 300 1234567"
              />
            </div>

            {/* License Document Upload */}
            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                <Upload className="inline mr-1" size={16} />
                Medical License Document *
              </label>
              <div className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center hover:border-blue-500 transition-colors">
                <input
                  type="file"
                  onChange={handleFileChange}
                  accept=".pdf,.jpg,.jpeg,.png"
                  className="hidden"
                  id="license-upload"
                  required
                />
                <label htmlFor="license-upload" className="cursor-pointer">
                  <Upload size={48} className="mx-auto text-gray-400 mb-2" />
                  {licenseFile ? (
                    <p className="text-sm text-gray-700">
                      <strong>Selected:</strong> {licenseFile.name}
                    </p>
                  ) : (
                    <>
                      <p className="text-sm text-gray-600">Click to upload license document</p>
                      <p className="text-xs text-gray-500 mt-1">PDF, JPG, PNG (Max 5MB)</p>
                    </>
                  )}
                </label>
              </div>
            </div>
          </div>

          {/* Info Box */}
          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
            <p className="text-sm text-yellow-800">
              <strong>⚠️ Document Verification Required</strong>
            </p>
            <ul className="text-sm text-yellow-700 mt-2 space-y-1 ml-4 list-disc">
              <li>Your CNIC and medical license will be verified by our admin team</li>
              <li>Verification usually takes 2-3 business days</li>
              <li>You'll receive an email once your documents are approved</li>
              <li>Ensure your license document is clear and readable</li>
            </ul>
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            {loading ? (
              <>
                <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                Submitting...
              </>
            ) : (
              'Register'
            )}
          </button>

          {/* Login Link */}
          <div className="text-center">
            <p className="text-sm text-gray-600">
              Already have an account?{' '}
              <Link to="/login" className="text-blue-600 hover:text-blue-700 font-medium">
                Login here
              </Link>
            </p>
          </div>
        </form>
      </div>
    </div>
  );
}