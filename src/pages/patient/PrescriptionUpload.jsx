// src/components/patient/PrescriptionUpload.jsx
import { useState } from 'react';
import { Upload, FileText } from 'lucide-react';
import { patientAPI } from '../../services/api';
import toast from 'react-hot-toast';

export default function PrescriptionUpload({ onUploadSuccess }) {
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);

  const handleFileChange = (e) => {
    const selectedFile = e.target.files[0];
    if (selectedFile) {
      // Validate file type
      if (!selectedFile.type.startsWith('image/')) {
        toast.error('Please select an image file');
        return;
      }
      setFile(selectedFile);
    }
  };

  const handleUpload = async () => {
    if (!file) {
      toast.error('Please select a file');
      return;
    }

    setUploading(true);
    const loadingToast = toast.loading('Processing prescription...');

    try {
      const formData = new FormData();
      formData.append('image', file);

      const response = await patientAPI.uploadPrescription(formData, {
        onUploadProgress: (progressEvent) => {
          const percentCompleted = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          );
          setProgress(percentCompleted);
        }
      });

      toast.success('Prescription uploaded successfully!', { id: loadingToast });
      setFile(null);
      setProgress(0);
      
      // ✅ Call parent callback to refresh list
      if (onUploadSuccess) {
        onUploadSuccess(response.data);
      }

    } catch (error) {
      console.error('Upload error:', error);
      toast.error(error.response?.data?.message || 'Upload failed', {
        id: loadingToast
      });
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h2 className="text-xl font-semibold mb-4 flex items-center gap-2">
        <Upload size={24} />
        Upload Prescription
      </h2>

      <div className="space-y-4">
        <div className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center">
          <input
            type="file"
            accept="image/*"
            onChange={handleFileChange}
            className="hidden"
            id="file-upload"
            disabled={uploading}
          />
          <label
            htmlFor="file-upload"
            className="cursor-pointer flex flex-col items-center"
          >
            <FileText size={48} className="text-gray-400 mb-2" />
            <span className="text-sm text-gray-600">
              {file ? file.name : 'Click to select prescription image'}
            </span>
          </label>
        </div>

        {uploading && (
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div
              className="bg-blue-600 h-2 rounded-full transition-all"
              style={{ width: `${progress}%` }}
            />
          </div>
        )}

        <button
          onClick={handleUpload}
          disabled={!file || uploading}
          className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed"
        >
          {uploading ? 'Processing...' : 'Upload & Process'}
        </button>
      </div>
    </div>
  );
}