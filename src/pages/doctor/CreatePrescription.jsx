// src/pages/doctor/CreatePrescription.jsx
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Plus, Trash2, FileText, User, Activity, 
  Thermometer, Scale, Heart, Pill, Calendar,
  Save, X
} from 'lucide-react';
import { doctorAPI, patientAPI } from '../../services/api';
import toast from 'react-hot-toast';

export default function CreatePrescription() {
  const navigate = useNavigate();
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    patientId: '',
    prescriptionDate: new Date().toISOString().split('T')[0],
    diagnosis: '',
    patientHistory: '',
    weight: '',
    temperature: '',
    bloodPressure: '',
    medicines: [
      {
        medicineName: '',
        medicineType: '',
        dosage: '',
        frequency: '',
        duration: '',
        quantity: ''
      }
    ]
  });

  useEffect(() => {
    fetchPatients();
  }, []);

  const fetchPatients = async () => {
    try {
      const response = await doctorAPI.getMyPatients();
      setPatients(response.data);
    } catch (error) {
      toast.error('Failed to load patients');
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleMedicineChange = (index, field, value) => {
    const updatedMedicines = [...formData.medicines];
    updatedMedicines[index][field] = value;
    setFormData(prev => ({
      ...prev,
      medicines: updatedMedicines
    }));
  };

  const addMedicine = () => {
    setFormData(prev => ({
      ...prev,
      medicines: [
        ...prev.medicines,
        {
          medicineName: '',
          medicineType: '',
          dosage: '',
          frequency: '',
          duration: '',
          quantity: ''
        }
      ]
    }));
  };

  const removeMedicine = (index) => {
    if (formData.medicines.length === 1) {
      toast.error('At least one medicine is required');
      return;
    }
    setFormData(prev => ({
      ...prev,
      medicines: prev.medicines.filter((_, i) => i !== index)
    }));
  };

  const validateForm = () => {
    if (!formData.patientId) {
      toast.error('Please select a patient');
      return false;
    }
    if (!formData.diagnosis) {
      toast.error('Diagnosis is required');
      return false;
    }
    if (formData.medicines.some(m => !m.medicineName)) {
      toast.error('All medicines must have a name');
      return false;
    }
    return true;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) return;

    setLoading(true);
    try {
      const response = await doctorAPI.createPrescription(formData);
      toast.success('Prescription created successfully!');
      
      // Navigate to patient's prescription list after 1 second
      setTimeout(() => {
        navigate(`/doctor/patients/${formData.patientId}/prescriptions`);
      }, 1000);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to create prescription');
    } finally {
      setLoading(false);
    }
  };

  const selectedPatient = patients.find(p => p.patientId === parseInt(formData.patientId));

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      {/* Header */}
      <div className="bg-white rounded-lg shadow p-6">
        <h1 className="text-2xl font-bold flex items-center gap-2">
          <FileText size={28} />
          Create Digital Prescription
        </h1>
        <p className="text-gray-600 mt-2">
          Fill in the prescription details for your patient
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Patient Selection */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold mb-4 flex items-center gap-2">
            <User size={24} />
            Patient Information
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Select Patient *
              </label>
              <select
                name="patientId"
                value={formData.patientId}
                onChange={handleInputChange}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              >
                <option value="">-- Select a patient --</option>
                {patients.map(patient => (
                  <option key={patient.patientId} value={patient.patientId}>
                    {patient.patientName} ({patient.patientUniqueId}) - Age: {patient.age}, {patient.gender}
                  </option>
                ))}
              </select>
            </div>

            {selectedPatient && (
              <div className="md:col-span-2 p-4 bg-blue-50 border border-blue-200 rounded-lg">
                <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
                  <div>
                    <span className="text-gray-600">Name:</span>
                    <p className="font-medium">{selectedPatient.patientName}</p>
                  </div>
                  <div>
                    <span className="text-gray-600">Age:</span>
                    <p className="font-medium">{selectedPatient.age} years</p>
                  </div>
                  <div>
                    <span className="text-gray-600">Gender:</span>
                    <p className="font-medium capitalize">{selectedPatient.gender}</p>
                  </div>
                  <div>
                    <span className="text-gray-600">ID:</span>
                    <p className="font-medium">{selectedPatient.patientUniqueId}</p>
                  </div>
                </div>
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                <Calendar className="inline mr-1" size={16} />
                Prescription Date
              </label>
              <input
                type="date"
                name="prescriptionDate"
                value={formData.prescriptionDate}
                onChange={handleInputChange}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
          </div>
        </div>

        {/* Vitals */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold mb-4 flex items-center gap-2">
            <Activity size={24} />
            Vital Signs
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                <Scale className="inline mr-1" size={16} />
                Weight (kg)
              </label>
              <input
                type="text"
                name="weight"
                value={formData.weight}
                onChange={handleInputChange}
                placeholder="e.g., 70"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                <Thermometer className="inline mr-1" size={16} />
                Temperature (°F)
              </label>
              <input
                type="text"
                name="temperature"
                value={formData.temperature}
                onChange={handleInputChange}
                placeholder="e.g., 98.6"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                <Heart className="inline mr-1" size={16} />
                Blood Pressure
              </label>
              <input
                type="text"
                name="bloodPressure"
                value={formData.bloodPressure}
                onChange={handleInputChange}
                placeholder="e.g., 120/80"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
          </div>
        </div>

        {/* Clinical Information */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold mb-4 flex items-center gap-2">
            <FileText size={24} />
            Clinical Information
          </h2>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Diagnosis *
              </label>
              <textarea
                name="diagnosis"
                value={formData.diagnosis}
                onChange={handleInputChange}
                rows={3}
                placeholder="Enter diagnosis..."
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Patient History / Notes
              </label>
              <textarea
                name="patientHistory"
                value={formData.patientHistory}
                onChange={handleInputChange}
                rows={3}
                placeholder="Enter patient history or additional notes..."
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
          </div>
        </div>

        {/* Medicines */}
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-semibold flex items-center gap-2">
              <Pill size={24} />
              Medicines
            </h2>
            <button
              type="button"
              onClick={addMedicine}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
            >
              <Plus size={18} />
              Add Medicine
            </button>
          </div>

          <div className="space-y-4">
            {formData.medicines.map((medicine, index) => (
              <div key={index} className="border border-gray-200 rounded-lg p-4 relative">
                <div className="flex items-center justify-between mb-3">
                  <h3 className="font-semibold text-gray-700">
                    Medicine #{index + 1}
                  </h3>
                  {formData.medicines.length > 1 && (
                    <button
                      type="button"
                      onClick={() => removeMedicine(index)}
                      className="text-red-600 hover:text-red-700 flex items-center gap-1"
                    >
                      <Trash2 size={16} />
                      Remove
                    </button>
                  )}
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Medicine Name *
                    </label>
                    <input
                      type="text"
                      value={medicine.medicineName}
                      onChange={(e) => handleMedicineChange(index, 'medicineName', e.target.value)}
                      placeholder="e.g., Paracetamol"
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Type
                    </label>
                    <select
                      value={medicine.medicineType}
                      onChange={(e) => handleMedicineChange(index, 'medicineType', e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    >
                      <option value="">Select type</option>
                      <option value="tablet">Tablet</option>
                      <option value="capsule">Capsule</option>
                      <option value="syrup">Syrup</option>
                      <option value="injection">Injection</option>
                      <option value="cream">Cream</option>
                      <option value="drops">Drops</option>
                      <option value="inhaler">Inhaler</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Dosage
                    </label>
                    <input
                      type="text"
                      value={medicine.dosage}
                      onChange={(e) => handleMedicineChange(index, 'dosage', e.target.value)}
                      placeholder="e.g., 500mg"
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Frequency
                    </label>
                    <input
                      type="text"
                      value={medicine.frequency}
                      onChange={(e) => handleMedicineChange(index, 'frequency', e.target.value)}
                      placeholder="e.g., 1+0+1"
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Duration
                    </label>
                    <input
                      type="text"
                      value={medicine.duration}
                      onChange={(e) => handleMedicineChange(index, 'duration', e.target.value)}
                      placeholder="e.g., 7 days"
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Quantity
                    </label>
                    <input
                      type="text"
                      value={medicine.quantity}
                      onChange={(e) => handleMedicineChange(index, 'quantity', e.target.value)}
                      placeholder="e.g., 14"
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Action Buttons */}
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex gap-4 justify-end">
            <button
              type="button"
              onClick={() => navigate('/doctor/dashboard')}
              className="px-6 py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 flex items-center gap-2"
            >
              <X size={20} />
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed flex items-center gap-2"
            >
              {loading ? (
                <>
                  <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                  Creating...
                </>
              ) : (
                <>
                  <Save size={20} />
                  Create Prescription
                </>
              )}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}