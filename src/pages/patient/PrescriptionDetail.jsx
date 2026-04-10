// src/pages/patient/PrescriptionDetail.jsx
import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  ArrowLeft, FileText, ImageIcon, CheckCircle, XCircle, 
  Pill, AlertTriangle
} from 'lucide-react';
import { patientAPI } from '../../services/api';
import toast from 'react-hot-toast';
import DrugWarningsSection from './DrugWarningssection.jsx';

export default function PrescriptionDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [prescription, setPrescription] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('results');
  const [imageUrl, setImageUrl] = useState(null);
  const [showImageModal, setShowImageModal] = useState(false);

  useEffect(() => {
    fetchPrescription();
  }, [id]);

  const fetchPrescription = async () => {
    try {
      const response = await patientAPI.getPrescriptionById(id);
      console.log('prescription data:', response.data.data);
      setPrescription(response.data.data);
    } catch (error) {
      toast.error('Failed to load prescription');
      navigate('/patient/dashboard');
    } finally {
      setLoading(false);
    }

    try {
      const imageBlob = await patientAPI.getPrescriptionImage(id);
      const imageObjectUrl = URL.createObjectURL(imageBlob.data);
      setImageUrl(imageObjectUrl);
    } catch (error) {
      console.error('Image load failed:', error);
    }
  };

  // MedicineResult DTO fields: rxcui, method, confidence, originalName, normalizedName
  const getRxNormStatusBadge = (medicine) => {
    if (medicine.rxcui || medicine.method === 'supplement_map') {
      return (
        <span className="inline-flex items-center gap-1 px-2 py-1 bg-green-100 text-green-800 text-xs rounded-full">
          <CheckCircle size={14} />
          {medicine.method === 'exact_match'    ? 'Exact Match' :
           medicine.method === 'fuzzy_match'    ? 'Fuzzy Match' :
           medicine.method === 'supplement_map' ? 'Supplement/Vaccine' : 'Matched'}
          {medicine.confidence > 0 &&
            ` (${Math.round(medicine.confidence * 100)}%)`}
        </span>
      );
    }
    return (
      <span className="inline-flex items-center gap-1 px-2 py-1 bg-red-100 text-red-800 text-xs rounded-full">
        <XCircle size={14} />
        No RxNorm Match
      </span>
    );
  };

  const renderMedicineRxNormDetails = (medicine) => {
    if (medicine.rxcui || medicine.method === 'supplement_map') {
      return (
        <div className="mt-3 p-3 bg-green-50 border border-green-200 rounded-lg">
          <div className="flex items-start gap-2 mb-2">
            <CheckCircle size={18} className="text-green-600 mt-0.5" />
            <div className="flex-1">
              <p className="font-semibold text-green-900">
                {medicine.method === 'supplement_map'
                  ? 'Supplement/Vaccine Identified'
                  : 'RxNorm Match Found'}
              </p>
              <p className="text-sm text-green-700">
                <span className="font-medium">Normalized Name:</span> {medicine.normalizedName}
              </p>
              {medicine.rxcui && (
                <p className="text-sm text-green-700">
                  <span className="font-medium">RxCUI:</span> {medicine.rxcui}
                </p>
              )}
              {medicine.method === 'supplement_map' && (
                <p className="text-sm text-green-700">
                  <span className="font-medium">Note:</span> Vaccine/supplement — not assigned a RxCUI
                </p>
              )}
            </div>
          </div>
        </div>
      );
    }
    return (
      <div className="mt-3 p-3 bg-red-50 border border-red-200 rounded-lg">
        <div className="flex items-start gap-2">
          <XCircle size={18} className="text-red-600 mt-0.5" />
          <div className="flex-1">
            <p className="font-semibold text-red-900">No RxNorm Match</p>
            <p className="text-sm text-red-700">
              The medicine "{medicine.originalName}" could not be matched to the RxNorm database.
              This may be due to:
            </p>
            <ul className="text-xs text-red-600 mt-1 space-y-0.5 pl-4">
              <li>• OCR error in reading the medicine name</li>
              <li>• Non-standard or brand-specific naming</li>
              <li>• Medicine not present in RxNorm database</li>
            </ul>
            <p className="text-xs text-red-700 mt-2">
              <strong>Recommendation:</strong> Please verify this medicine name with your doctor.
            </p>
          </div>
        </div>
      </div>
    );
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!prescription) {
    return <div>Prescription not found</div>;
  }

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <button
          onClick={() => navigate('/patient/dashboard')}
          className="flex items-center gap-2 text-gray-600 hover:text-gray-900"
        >
          <ArrowLeft size={20} />
          Back to Dashboard
        </button>

        <div className="flex gap-2">
          {/* FIX: prescription.status not prescription.processingStatus */}
          <span className={`px-3 py-1 rounded-full text-sm ${
            prescription.status === 'COMPLETED'
              ? 'bg-green-100 text-green-800'
              : prescription.status === 'PROCESSING'
              ? 'bg-yellow-100 text-yellow-800'
              : 'bg-red-100 text-red-800'
          }`}>
            {prescription.status}
          </span>
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-white rounded-lg shadow">
        <div className="border-b border-gray-200">
          <nav className="flex -mb-px">
            <button
              onClick={() => setActiveTab('results')}
              className={`px-6 py-3 border-b-2 font-medium text-sm ${
                activeTab === 'results'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <FileText size={18} className="inline mr-2" />
              View Results
            </button>
            <button
              onClick={() => setActiveTab('image')}
              className={`px-6 py-3 border-b-2 font-medium text-sm ${
                activeTab === 'image'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <ImageIcon size={18} className="inline mr-2" />
              View Image
            </button>
          </nav>
        </div>

        <div className="p-6">
          {activeTab === 'results' ? (
            <div className="space-y-6">
              {/* Medicines */}
              <div className="bg-white rounded-lg border border-gray-200 p-6">
                <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
                  <Pill size={20} />
                  Prescribed Medicines
                </h3>
                {prescription.medicines && prescription.medicines.length > 0 ? (
                  <div className="space-y-4">
                    {prescription.medicines.map((medicine, index) => (
                      // FIX: MedicineResult has no medicineId — use index as key
                      <div key={index} className="border border-gray-200 rounded-lg p-4">
                        <div className="flex items-start justify-between mb-2">
                          <div className="flex items-start gap-3">
                            <span className="flex items-center justify-center w-8 h-8 bg-blue-100 text-blue-600 rounded-full font-semibold text-sm">
                              {index + 1}
                            </span>
                            <div>
                              {/* FIX: originalName not medicineName */}
                              <h4 className="font-semibold text-lg">{medicine.originalName}</h4>
                              {medicine.medicineType && (
                                <p className="text-sm text-gray-600 capitalize">{medicine.medicineType}</p>
                              )}
                            </div>
                          </div>
                          {getRxNormStatusBadge(medicine)}
                        </div>

                        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mt-3 text-sm">
                          {medicine.dosage && (
                            <div>
                              <span className="text-gray-600">Dosage:</span>
                              <p className="font-medium">{medicine.dosage}</p>
                            </div>
                          )}
                          {medicine.frequency && (
                            <div>
                              <span className="text-gray-600">Frequency:</span>
                              <p className="font-medium">{medicine.frequency}</p>
                            </div>
                          )}
                          {medicine.duration && (
                            <div>
                              <span className="text-gray-600">Duration:</span>
                              <p className="font-medium">{medicine.duration}</p>
                            </div>
                          )}
                          {medicine.quantity && (
                            <div>
                              <span className="text-gray-600">Quantity:</span>
                              <p className="font-medium">{medicine.quantity}</p>
                            </div>
                          )}
                        </div>

                        {renderMedicineRxNormDetails(medicine)}
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-gray-500 text-center py-4">No medicines found</p>
                )}
              </div>

              {/* Duplicate Ingredient Warnings */}
              {(prescription.duplicateIngredientWarnings ?? []).length > 0 && (
                <div className="bg-white rounded-lg border border-yellow-300 p-6">
                  <h3 className="text-lg font-semibold mb-4 flex items-center gap-2 text-yellow-800">
                    <AlertTriangle size={20} className="text-yellow-500" />
                    Duplicate Ingredient Notice
                  </h3>
                  <div className="space-y-3">
                    {prescription.duplicateIngredientWarnings.map((warning, index) => (
                      <div
                        key={index}
                        className="flex items-start gap-3 p-3 bg-yellow-50 border border-yellow-200 rounded-lg"
                      >
                        <AlertTriangle size={18} className="text-yellow-600 mt-0.5 shrink-0" />
                        <p className="text-sm text-yellow-800">{warning.message}</p>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Drug Warnings */}
              <div className="bg-white rounded-lg border border-gray-200 p-6">
                <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
                  <AlertTriangle size={20} className="text-amber-500" />
                  Drug Safety Analysis
                </h3>
                <DrugWarningsSection
                  drugInteractions={prescription.drugInteractions ?? []}
                  drugDiseaseWarnings={prescription.drugDiseaseWarnings ?? []}
                  highSeverityCount={prescription.highSeverityCount ?? 0}
                  moderateSeverityCount={prescription.moderateSeverityCount ?? 0}
                />
              </div>
            </div>
          ) : (
            <div className="space-y-4">
              {imageUrl ? (
                <>
                  <div className="flex justify-between items-center">
                    <h3 className="text-lg font-semibold">Original Prescription Image</h3>
                    <a
                      href={imageUrl}
                      download="prescription.jpg"
                      target="_blank"
                      rel="noopener noreferrer"
                      className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                    >
                      Download
                    </a>
                  </div>
                  <div
                    className="border border-gray-300 rounded-lg overflow-hidden cursor-pointer hover:shadow-lg transition-shadow"
                    onClick={() => setShowImageModal(true)}
                  >
                    <img src={imageUrl} alt="Prescription" className="w-full h-auto" />
                  </div>
                  <p className="text-sm text-gray-500 text-center">Click image to enlarge</p>
                </>
              ) : (
                <p className="text-center text-gray-500 py-8">Image not available</p>
              )}
            </div>
          )}
        </div>
      </div>

      {showImageModal && (
        <div
          className="fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center z-50 p-4"
          onClick={() => setShowImageModal(false)}
        >
          <div className="max-w-6xl max-h-full">
            <img
              src={imageUrl}
              alt="Prescription"
              className="max-w-full max-h-screen object-contain"
            />
          </div>
        </div>
      )}
    </div>
  );
}