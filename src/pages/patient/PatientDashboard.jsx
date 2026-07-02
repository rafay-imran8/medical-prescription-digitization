import { useState, useEffect } from 'react';
import { patientAPI } from '../../services/api';
import { 
  Upload, FileText, Calendar, AlertCircle, 
  CheckCircle, Clock, Eye
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { formatDate, getStatusColor, validateImageFile } from '../../utils/helpers';

const PatientDashboard = () => {
  const [prescriptions, setPrescriptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    fetchPrescriptions();
  }, []);

  const fetchPrescriptions = async () => {
    try {
      const response = await patientAPI.getPrescriptions();
      setPrescriptions(response.data.data);
    } catch (error) {
      toast.error('Failed to load prescriptions');
    } finally {
      setLoading(false);
    }
  };

  const handleFileSelect = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const validation = validateImageFile(file);
    if (!validation.valid) {
      toast.error(validation.error);
      return;
    }

    setSelectedFile(file);
    setPreviewUrl(URL.createObjectURL(file));
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      toast.error('Please select an image');
      return;
    }

    setUploading(true);
    const formData = new FormData();
    formData.append('image', selectedFile);

    try {
      await patientAPI.uploadPrescription(formData);
      toast.success('Prescription uploaded and processing!');
      setSelectedFile(null);
      setPreviewUrl(null);
      fetchPrescriptions();
    } catch (error) {
      toast.error('Failed to upload prescription');
    } finally {
      setUploading(false);
    }
  };

  const getStatusIcon = (status) => {
    switch (status?.toLowerCase()) {
      case 'completed':
        return <CheckCircle className="h-5 w-5 text-green-600" />;
      case 'processing':
        return <Clock className="h-5 w-5 text-yellow-600" />;
      case 'failed':
        return <AlertCircle className="h-5 w-5 text-red-600" />;
      default:
        return <FileText className="h-5 w-5 text-gray-600" />;
    }
  };

  if (loading) {
    return <LoadingSpinner fullScreen />;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">My Prescriptions</h1>
          <p className="text-gray-600 mt-2">
            Upload and manage your prescription records
          </p>
        </div>

        {/* Upload Section */}
        <div className="card mb-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">
            Upload New Prescription
          </h2>
          
          <div className="grid md:grid-cols-2 gap-6">
            {/* File Upload */}
            <div>
              <label className="block w-full">
                <input
                  type="file"
                  accept="image/jpeg,image/jpg,image/png"
                  onChange={handleFileSelect}
                  className="hidden"
                  disabled={uploading}
                />
                <div className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center cursor-pointer hover:border-primary-500 transition-colors">
                  <Upload className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                  <p className="text-sm text-gray-600">
                    {selectedFile ? selectedFile.name : 'Click to select image'}
                  </p>
                  <p className="text-xs text-gray-500 mt-2">
                    JPG, PNG (Max 10MB)
                  </p>
                </div>
              </label>

              <button
                onClick={handleUpload}
                disabled={!selectedFile || uploading}
                className="btn-primary w-full mt-4"
              >
                {uploading ? 'Uploading & Processing...' : 'Upload Prescription'}
              </button>
            </div>

            {/* Preview */}
            {previewUrl && (
              <div>
                <p className="text-sm font-medium text-gray-700 mb-2">Preview:</p>
                <img
                  src={previewUrl}
                  alt="Preview"
                  className="w-full h-64 object-contain bg-gray-100 rounded-lg"
                />
              </div>
            )}
          </div>

          {uploading && (
            <div className="mt-6 p-4 bg-blue-50 rounded-lg">
              <div className="flex items-center space-x-3">
                <LoadingSpinner size="sm" />
                <div>
                  <p className="text-sm font-medium text-blue-900">
                    Processing your prescription...
                  </p>
                  <p className="text-xs text-blue-700 mt-1">
                    AI is extracting data (OCR → LLM → RxNorm → Drug Interactions)
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Prescriptions List */}
        <div className="card">
          <h2 className="text-xl font-semibold text-gray-900 mb-6">
            Previous Prescriptions ({prescriptions.length})
          </h2>

          {prescriptions.length === 0 ? (
            <div className="text-center py-12">
              <FileText className="h-16 w-16 text-gray-300 mx-auto mb-4" />
              <p className="text-gray-600">No prescriptions yet</p>
              <p className="text-sm text-gray-500 mt-2">
                Upload your first prescription above
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {prescriptions.map((prescription) => (
                <div
                  // FIX: use prescription.id — DTO serializes as "id" via @JsonProperty
                  key={prescription.id}
                  className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow cursor-pointer"
                  onClick={() => navigate(`/patient/prescriptions/${prescription.id}`)}
                >
                  <div className="flex items-start justify-between">
                    <div className="flex items-start space-x-4 flex-1">
                      {getStatusIcon(prescription.status)}
                      
                      <div className="flex-1">
                        <div className="flex items-center space-x-3 mb-2">
                          <h3 className="font-semibold text-gray-900">
                            {/* FIX: prescription.id, not prescription.prescriptionId */}
                            Prescription #{prescription.id}
                          </h3>
                          <span className={`badge ${getStatusColor(prescription.status)}`}>
                            {prescription.status}
                          </span>
                        </div>

                        <div className="grid grid-cols-2 gap-4 text-sm">
                          <div>
                            <p className="text-gray-600">
                              <Calendar className="h-4 w-4 inline mr-1" />
                              {formatDate(prescription.createdAt)}
                            </p>
                          </div>
                          <div>
                            <p className="text-gray-600">
                              Medicines: {prescription.medicines?.length || 0}
                            </p>
                          </div>
                        </div>

                        {/* FIX: drugInteractions, not interactions */}
                        {prescription.drugInteractions && prescription.drugInteractions.length > 0 && (
                          <div className="mt-2 flex items-center space-x-2">
                            <AlertCircle className="h-4 w-4 text-yellow-600" />
                            <span className="text-xs text-yellow-700">
                              {prescription.drugInteractions.length} drug interaction(s) detected
                            </span>
                          </div>
                        )}
                      </div>
                    </div>

                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        navigate(`/patient/prescriptions/${prescription.id}`);
                      }}
                      className="btn-secondary flex items-center space-x-1"
                    >
                      <Eye className="h-4 w-4" />
                      <span>View</span>
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

export default PatientDashboard;