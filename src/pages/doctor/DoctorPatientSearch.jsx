// src/pages/doctor/DoctorPatientSearch.jsx
import { useState } from 'react';
import { Search, User, Send, CheckCircle, AlertCircle } from 'lucide-react';
import { doctorAPI } from '../../services/api';
import toast from 'react-hot-toast';

export default function DoctorPatientSearch() {
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [requestingAccess, setRequestingAccess] = useState(null);

  const handleSearch = async (e) => {
    e.preventDefault();
    
    if (!searchQuery.trim()) {
      toast.error('Please enter a patient ID');
      return;
    }

    setSearching(true);
    try {
      const response = await doctorAPI.searchPatients(searchQuery);
      setSearchResults(response.data);
      
      if (response.data.length === 0) {
        toast.error('No patients found with that ID');
      }
    } catch (error) {
      toast.error('Search failed');
      console.error('Search error:', error);
    } finally {
      setSearching(false);
    }
  };

  const handleRequestAccess = async (patientId) => {
    setRequestingAccess(patientId);
    try {
      const response = await doctorAPI.requestAccess(patientId);
      toast.success(response.data.message || 'Access request sent successfully');
      
      // Update the local state to show request sent
      setSearchResults(prev => 
        prev.map(p => 
          p.patientId === patientId 
            ? { ...p, accessRequested: true }
            : p
        )
      );
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to request access');
    } finally {
      setRequestingAccess(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-2xl font-bold mb-6 flex items-center gap-2">
          <Search size={28} />
          Search Patient Records
        </h2>

        {/* Search Form */}
        <form onSubmit={handleSearch} className="mb-6">
          <div className="flex gap-3">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Enter Patient ID (e.g., PAT-00001)"
                className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
            <button
              type="submit"
              disabled={searching}
              className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed flex items-center gap-2"
            >
              {searching ? (
                <>
                  <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                  Searching...
                </>
              ) : (
                <>
                  <Search size={20} />
                  Search
                </>
              )}
            </button>
          </div>
        </form>

        {/* Search Results */}
        {searchResults.length > 0 && (
          <div className="space-y-4">
            <h3 className="text-lg font-semibold text-gray-700">
              Search Results ({searchResults.length})
            </h3>
            
            {searchResults.map((patient) => (
              <div
                key={patient.patientId}
                className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-4">
                    <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center">
                      <User size={24} className="text-blue-600" />
                    </div>
                    <div>
                      <h4 className="font-semibold text-lg">{patient.patientName}</h4>
                      <p className="text-sm text-gray-600">ID: {patient.patientUniqueId}</p>
                      <div className="flex gap-4 mt-1 text-sm text-gray-500">
                        <span>Age: {patient.age}</span>
                        <span>Gender: {patient.gender}</span>
                      </div>
                    </div>
                  </div>

                  <div>
                    {patient.hasAccess ? (
                      <span className="inline-flex items-center gap-1 px-4 py-2 bg-green-100 text-green-800 rounded-lg">
                        <CheckCircle size={18} />
                        Access Granted
                      </span>
                    ) : patient.accessRequested ? (
                      <span className="inline-flex items-center gap-1 px-4 py-2 bg-yellow-100 text-yellow-800 rounded-lg">
                        <AlertCircle size={18} />
                        Request Pending
                      </span>
                    ) : (
                      <button
                        onClick={() => handleRequestAccess(patient.patientId)}
                        disabled={requestingAccess === patient.patientId}
                        className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
                      >
                        {requestingAccess === patient.patientId ? (
                          <>
                            <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                            Requesting...
                          </>
                        ) : (
                          <>
                            <Send size={18} />
                            Request Access
                          </>
                        )}
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* No Results */}
        {!searching && searchResults.length === 0 && searchQuery && (
          <div className="text-center py-12 text-gray-500">
            <Search size={48} className="mx-auto mb-3 text-gray-300" />
            <p>No patients found matching "{searchQuery}"</p>
            <p className="text-sm mt-2">Try searching with a different Patient ID</p>
          </div>
        )}

        {/* Instructions */}
        {searchResults.length === 0 && !searchQuery && (
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
            <h3 className="font-semibold text-blue-900 mb-3 flex items-center gap-2">
              <AlertCircle size={20} />
              How to Request Patient Access
            </h3>
            <ol className="list-decimal list-inside space-y-2 text-blue-800 text-sm">
              <li>Enter the patient's unique ID (e.g., PAT-00001) in the search box</li>
              <li>Click "Request Access" next to the patient you want to view</li>
              <li>The patient will receive an email notification</li>
              <li>Once the patient approves, you'll receive an access code via email</li>
              <li>Enter the code in the "Activate Access" section</li>
              <li>Access will be valid for 24 hours</li>
            </ol>
          </div>
        )}
      </div>
    </div>
  );
}