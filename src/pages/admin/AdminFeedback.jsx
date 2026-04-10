// src/pages/admin/AdminFeedback.jsx
import { useState, useEffect } from 'react';
import { MessageSquare, User, Calendar, Star } from 'lucide-react';
import { adminAPI } from '../../services/api';
import toast from 'react-hot-toast';

export default function AdminFeedback() {
  const [feedbacks, setFeedbacks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ALL');

  useEffect(() => {
    fetchFeedbacks();
  }, []);

  const fetchFeedbacks = async () => {
    try {
      const response = await adminAPI.getAllFeedbacks();
      setFeedbacks(response.data.data);
    } catch (error) {
      toast.error('Failed to load feedbacks');
    } finally {
      setLoading(false);
    }
  };

  const updateStatus = async (feedbackId, status) => {
    try {
      await adminAPI.updateFeedbackStatus(feedbackId, status);
      toast.success('Status updated');
      fetchFeedbacks();
    } catch (error) {
      toast.error('Failed to update status');
    }
  };

  const filteredFeedbacks = feedbacks.filter(f => 
    filter === 'ALL' || f.status === filter
  );

  const renderStars = (rating) => {
    return (
      <div className="flex gap-1">
        {[1, 2, 3, 4, 5].map((star) => (
          <Star
            key={star}
            size={16}
            className={star <= rating ? 'fill-yellow-400 text-yellow-400' : 'text-gray-300'}
          />
        ))}
      </div>
    );
  };

  const getStatusColor = (status) => {
    const colors = {
      PENDING: 'bg-yellow-100 text-yellow-800',
      REVIEWED: 'bg-blue-100 text-blue-800',
      RESOLVED: 'bg-green-100 text-green-800',
      DISMISSED: 'bg-gray-100 text-gray-800'
    };
    return colors[status] || colors.PENDING;
  };

  if (loading) {
    return <div className="flex justify-center p-8">Loading...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold flex items-center gap-2">
          <MessageSquare size={28} />
          User Feedback
        </h1>

        <div className="flex gap-2">
          {['ALL', 'PENDING', 'REVIEWED', 'RESOLVED'].map((status) => (
            <button
              key={status}
              onClick={() => setFilter(status)}
              className={`px-4 py-2 rounded-lg ${
                filter === status
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
              }`}
            >
              {status}
            </button>
          ))}
        </div>
      </div>

      <div className="grid gap-4">
        {filteredFeedbacks.map((feedback) => (
          <div key={feedback.feedbackId} className="bg-white rounded-lg shadow p-6">
            <div className="flex justify-between items-start mb-4">
              <div className="flex items-center gap-3">
                <User size={20} className="text-gray-600" />
                <div>
                  <p className="font-semibold">{feedback.userName}</p>
                  <p className="text-sm text-gray-500">{feedback.feedbackType}</p>
                </div>
              </div>

              <div className="flex items-center gap-3">
                {renderStars(feedback.rating)}
                <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(feedback.status)}`}>
                  {feedback.status}
                </span>
              </div>
            </div>

            <div className="mb-4">
              <h3 className="font-semibold mb-2">{feedback.subject}</h3>
              <p className="text-gray-700">{feedback.message}</p>
            </div>

            <div className="flex justify-between items-center text-sm text-gray-500">
              <span className="flex items-center gap-1">
                <Calendar size={14} />
                {new Date(feedback.createdAt).toLocaleDateString()}
              </span>

              {feedback.status === 'PENDING' && (
                <div className="flex gap-2">
                  <button
                    onClick={() => updateStatus(feedback.feedbackId, 'REVIEWED')}
                    className="px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700"
                  >
                    Mark Reviewed
                  </button>
                  <button
                    onClick={() => updateStatus(feedback.feedbackId, 'RESOLVED')}
                    className="px-3 py-1 bg-green-600 text-white rounded hover:bg-green-700"
                  >
                    Resolve
                  </button>
                </div>
              )}
            </div>
          </div>
        ))}
      </div>

      {filteredFeedbacks.length === 0 && (
        <div className="text-center py-12 text-gray-500">
          No {filter.toLowerCase()} feedbacks found
        </div>
      )}
    </div>
  );
}