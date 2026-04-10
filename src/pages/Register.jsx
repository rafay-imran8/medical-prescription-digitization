import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { 
  FileText, Mail, Lock, User, Phone, MapPin, 
  UserCircle, Stethoscope, Calendar, Users
} from 'lucide-react';
import LoadingSpinner from '../components/common/LoadingSpinner';
import { getPasswordStrength } from '../utils/helpers';

const Register = () => {
  const [step, setStep] = useState(1); // 1: Role selection, 2: Form
  const [role, setRole] = useState('');
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    confirmPassword: '',
    fullName: '',
    // Patient fields
    age: '',
    gender: '',
    contactNumber: '',
    address: '',
    // Doctor fields
    specialization: '',
    licenseNumber: '',
    phone: '',
    clinicAddress: '',
  });
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const passwordStrength = getPasswordStrength(formData.password);

  const handleRoleSelect = (selectedRole) => {
    setRole(selectedRole);
    setStep(2);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (formData.password !== formData.confirmPassword) {
      toast.error('Passwords do not match');
      return;
    }

    setLoading(true);

    try {
      const data = {
        email: formData.email,
        password: formData.password,
        fullName: formData.fullName,
        role: role,
      };

      // Add role-specific fields
      if (role === 'PATIENT') {
        data.age = parseInt(formData.age) || null;
        data.gender = formData.gender;
        data.contactNumber = formData.contactNumber;
        data.address = formData.address;
      } else if (role === 'DOCTOR') {
        data.specialization = formData.specialization;
        data.licenseNumber = formData.licenseNumber;
        data.phone = formData.phone;
        data.clinicAddress = formData.clinicAddress;
      }

      const user = await register(data);

      // Navigate based on role
      switch (user.role) {
        case 'PATIENT':
          navigate('/patient/dashboard');
          break;
        case 'DOCTOR':
          navigate('/doctor/dashboard');
          break;
        default:
          navigate('/');
      }
    } catch (error) {
      console.error('Registration error:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  // Role Selection Screen
  if (step === 1) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-primary-50 to-blue-100 flex items-center justify-center px-4">
        <div className="max-w-4xl w-full">
          <div className="text-center mb-8">
            <div className="flex justify-center mb-4">
              <div className="bg-primary-600 p-3 rounded-xl">
                <FileText className="h-12 w-12 text-white" />
              </div>
            </div>
            <h1 className="text-3xl font-bold text-gray-900">Create Account</h1>
            <p className="text-gray-600 mt-2">Select your role to get started</p>
          </div>

          <div className="grid md:grid-cols-2 gap-6">
            {/* Patient Card */}
            <button
              onClick={() => handleRoleSelect('PATIENT')}
              className="card hover:shadow-lg transition-all cursor-pointer text-left group"
            >
              <div className="flex items-center space-x-4 mb-4">
                <div className="bg-blue-100 p-3 rounded-lg group-hover:bg-blue-200 transition-colors">
                  <UserCircle className="h-8 w-8 text-blue-600" />
                </div>
                <div>
                  <h3 className="text-xl font-semibold text-gray-900">Patient</h3>
                  <p className="text-sm text-gray-600">Upload & manage prescriptions</p>
                </div>
              </div>
              <ul className="space-y-2 text-sm text-gray-600">
                <li>• Upload prescription images</li>
                <li>• AI-powered OCR processing</li>
                <li>• View medical history</li>
                <li>• Grant doctor access</li>
              </ul>
            </button>

            {/* Doctor Card */}
            <button
              onClick={() => handleRoleSelect('DOCTOR')}
              className="card hover:shadow-lg transition-all cursor-pointer text-left group"
            >
              <div className="flex items-center space-x-4 mb-4">
                <div className="bg-green-100 p-3 rounded-lg group-hover:bg-green-200 transition-colors">
                  <Stethoscope className="h-8 w-8 text-green-600" />
                </div>
                <div>
                  <h3 className="text-xl font-semibold text-gray-900">Doctor</h3>
                  <p className="text-sm text-gray-600">Access patient records</p>
                </div>
              </div>
              <ul className="space-y-2 text-sm text-gray-600">
                <li>• Request patient access</li>
                <li>• View patient prescriptions</li>
                <li>• Write digital prescriptions</li>
                <li>• Monitor patient health</li>
              </ul>
            </button>
          </div>

          <div className="text-center mt-6">
            <Link
              to="/login"
              className="text-primary-600 font-medium hover:text-primary-700"
            >
              Already have an account? Sign in
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // Registration Form
  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-50 to-blue-100 flex items-center justify-center px-4 py-8">
      <div className="max-w-2xl w-full">
        <div className="text-center mb-6">
          <h1 className="text-3xl font-bold text-gray-900">
            Register as {role === 'PATIENT' ? 'Patient' : 'Doctor'}
          </h1>
          <button
            onClick={() => setStep(1)}
            className="text-sm text-primary-600 hover:text-primary-700 mt-2"
          >
            ← Change role
          </button>
        </div>

        <div className="card">
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Common Fields */}
            <div className="grid md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Full Name *
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <User className="h-5 w-5 text-gray-400" />
                  </div>
                  <input
                    type="text"
                    name="fullName"
                    value={formData.fullName}
                    onChange={handleChange}
                    className="input-field pl-10"
                    placeholder="John Doe"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Email Address *
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Mail className="h-5 w-5 text-gray-400" />
                  </div>
                  <input
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    className="input-field pl-10"
                    placeholder="you@example.com"
                    required
                  />
                </div>
              </div>
            </div>

            <div className="grid md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Password *
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Lock className="h-5 w-5 text-gray-400" />
                  </div>
                  <input
                    type="password"
                    name="password"
                    value={formData.password}
                    onChange={handleChange}
                    className="input-field pl-10"
                    placeholder="••••••••"
                    required
                  />
                </div>
                {formData.password && (
                  <div className="mt-2">
                    <div className="flex items-center space-x-2">
                      <div className="flex-1 h-2 bg-gray-200 rounded-full overflow-hidden">
                        <div
                          className={`h-full transition-all ${
                            passwordStrength.color === 'red'
                              ? 'bg-red-500'
                              : passwordStrength.color === 'yellow'
                              ? 'bg-yellow-500'
                              : 'bg-green-500'
                          }`}
                          style={{ width: `${(passwordStrength.strength / 5) * 100}%` }}
                        ></div>
                      </div>
                      <span className="text-xs text-gray-600">
                        {passwordStrength.label}
                      </span>
                    </div>
                  </div>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Confirm Password *
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Lock className="h-5 w-5 text-gray-400" />
                  </div>
                  <input
                    type="password"
                    name="confirmPassword"
                    value={formData.confirmPassword}
                    onChange={handleChange}
                    className="input-field pl-10"
                    placeholder="••••••••"
                    required
                  />
                </div>
              </div>
            </div>

            {/* Patient-specific fields */}
            {role === 'PATIENT' && (
              <>
                <div className="grid md:grid-cols-3 gap-6">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Age
                    </label>
                    <input
                      type="number"
                      name="age"
                      value={formData.age}
                      onChange={handleChange}
                      className="input-field"
                      placeholder="30"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Gender
                    </label>
                    <select
                      name="gender"
                      value={formData.gender}
                      onChange={handleChange}
                      className="input-field"
                    >
                      <option value="">Select</option>
                      <option value="Male">Male</option>
                      <option value="Female">Female</option>
                      <option value="Other">Other</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Contact Number
                    </label>
                    <input
                      type="tel"
                      name="contactNumber"
                      value={formData.contactNumber}
                      onChange={handleChange}
                      className="input-field"
                      placeholder="+1234567890"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Address
                  </label>
                  <textarea
                    name="address"
                    value={formData.address}
                    onChange={handleChange}
                    className="input-field"
                    rows="3"
                    placeholder="Street address, city, state, zip"
                  />
                </div>
              </>
            )}

            {/* Doctor-specific fields */}
            {role === 'DOCTOR' && (
              <>
                <div className="grid md:grid-cols-2 gap-6">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Specialization *
                    </label>
                    <input
                      type="text"
                      name="specialization"
                      value={formData.specialization}
                      onChange={handleChange}
                      className="input-field"
                      placeholder="Cardiology"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      License Number *
                    </label>
                    <input
                      type="text"
                      name="licenseNumber"
                      value={formData.licenseNumber}
                      onChange={handleChange}
                      className="input-field"
                      placeholder="LIC-12345"
                      required
                    />
                  </div>
                </div>

                <div className="grid md:grid-cols-2 gap-6">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Phone Number
                    </label>
                    <input
                      type="tel"
                      name="phone"
                      value={formData.phone}
                      onChange={handleChange}
                      className="input-field"
                      placeholder="+1234567890"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Clinic Address
                    </label>
                    <input
                      type="text"
                      name="clinicAddress"
                      value={formData.clinicAddress}
                      onChange={handleChange}
                      className="input-field"
                      placeholder="Clinic location"
                    />
                  </div>
                </div>
              </>
            )}

            {/* Submit Button */}
            <button
              type="submit"
              disabled={loading}
              className="btn-primary w-full flex items-center justify-center space-x-2"
            >
              {loading ? (
                <LoadingSpinner size="sm" />
              ) : (
                <>
                  <Users className="h-5 w-5" />
                  <span>Create Account</span>
                </>
              )}
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-gray-600">
              Already have an account?{' '}
              <Link
                to="/login"
                className="text-primary-600 font-medium hover:text-primary-700"
              >
                Sign in
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Register;