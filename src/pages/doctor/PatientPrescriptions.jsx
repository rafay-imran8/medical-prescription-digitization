import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { doctorAPI } from '../../services/api';
import { 
  ArrowLeft, FileText, Calendar, AlertCircle, 
  Eye, User
} from 'lucide-react';
import toast from 'react-hot-toast';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { formatDate, getStatusColor } from '../../utils/helpers';

const PatientPrescriptions = () => {
  const { patientId } = useParams();
  const navigate = useNavigate();
  const [prescriptions, setPrescriptions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchPrescriptions();
  }, [patientId]);

  const fetchPrescriptions = async () => {
    try {
      const response = await doctorAPI.getPatientPrescriptions(patientId);
      setPrescriptions(response.data.data);
    } catch (error) {
      toast.error('Failed to load prescriptions');
      navigate('/doctor/dashboard');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <LoadingSpinner fullScreen />;
  }

  const patient = prescriptions[0]?.patientInfo;

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-6">
          <button
            onClick={() => navigate('/doctor/dashboard')}
            className="flex items-center space-x-2 text-gray-600 hover:text-gray-900 mb-4"
          >
            <ArrowLeft className="h-5 w-5" />
            <span>Back to Patients</span>
          </button>
          
          {patient && (
            <div className="card mb-6">
              <div className="flex items-center space-x-4">
                <div className="bg-primary-100 p-4 rounded-lg">
                  <User className="h-8 w-8 text-primary-600" />
                </div>
                <div>
                  <h1 className="text-2xl font-bold text-gray-900">
                    {patient.patientName}
                  </h1>
                  <p className="text-gray-600">
                    {patient.age} years • {patient.gender} • ID: {patient.patientUniqueId}
                  </p>
                </div>
              </div>
            </div>
          )}

          <h2 className="text-xl font-semibold text-gray-900">
            Prescription History
          </h2>
        </div>

        {/* Prescriptions */}
        <div className="card">
          {prescriptions.length === 0 ? (
            <div className="text-center py-12">
              <FileText className="h-16 w-16 text-gray-300 mx-auto mb-4" />
              <p className="text-gray-600">No prescriptions found</p>
              <p className="text-sm text-gray-500 mt-2">
                This patient hasn't uploaded any prescriptions yet
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {prescriptions.map((prescription) => (
                <div
                  key={prescription.prescriptionId}
                  className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow"
                >
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center space-x-3 mb-3">
                        <h3 className="font-semibold text-gray-900">
                          Prescription #{prescription.prescriptionId}
                        </h3>
                        <span className={`badge ${getStatusColor(prescription.processingStatus)}`}>
                          {prescription.processingStatus}
                        </span>
                      </div>

                      <div className="grid md:grid-cols-3 gap-4 text-sm mb-3">
                        <div>
                          <p className="text-gray-600">
                            <Calendar className="h-4 w-4 inline mr-1" />
                            {formatDate(prescription.prescriptionDate)}
                          </p>
                        </div>
                        <div>
                          <p className="text-gray-600">
                            Medicines: {prescription.medicines?.length || 0}
                          </p>
                        </div>
                        <div>
                          <p className="text-gray-600">
                            Type: {prescription.prescriptionType}
                          </p>
                        </div>
                      </div>

                      {prescription.diagnosis && (
                        <div className="mb-3">
                          <p className="text-sm text-gray-600">
                            <strong>Diagnosis:</strong> {prescription.diagnosis}
                          </p>
                        </div>
                      )}

                      {prescription.medicines && prescription.medicines.length > 0 && (
                        <div className="bg-gray-50 rounded-lg p-3 mb-3">
                          <p className="text-sm font-medium text-gray-900 mb-2">
                            Medicines:
                          </p>
                          <div className="space-y-1">
                            {prescription.medicines.slice(0, 3).map((med, index) => (
                              <p key={index} className="text-sm text-gray-700">
                                • {med.medicineName} 
                                {med.dosage && ` - ${med.dosage}`}
                                {med.frequency && ` - ${med.frequency}`}
                              </p>
                            ))}
                            {prescription.medicines.length > 3 && (
                              <p className="text-sm text-gray-500">
                                +{prescription.medicines.length - 3} more
                              </p>
                            )}
                          </div>
                        </div>
                      )}

                      {prescription.interactions && prescription.interactions.length > 0 && (
                        <div className="flex items-center space-x-2 text-yellow-700 bg-yellow-50 rounded-lg p-2">
                          <AlertCircle className="h-4 w-4" />
                          <span className="text-sm">
                            {prescription.interactions.length} drug interaction(s) detected
                          </span>
                        </div>
                      )}
                    </div>

                    <button
                      onClick={() => navigate(`/doctor/prescriptions/${prescription.prescriptionId}`)}
                      className="btn-primary flex items-center space-x-2 ml-4"
                    >
                      <Eye className="h-4 w-4" />
                      <span>View Details</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default PatientPrescriptions;