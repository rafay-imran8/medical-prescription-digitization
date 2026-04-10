import { useState } from 'react';
import { Star, MessageSquare, Send, X } from 'lucide-react';
import axios from 'axios';
import toast from 'react-hot-toast';

const FeedbackModal = ({ isOpen, onClose }) => {
  const [formData, setFormData] = useState({
    feedbackType: 'SYSTEM',
    rating: 0,
    subject: '',
    message: ''
  });
  const [hoveredRating, setHoveredRating] = useState(0);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (formData.rating === 0) {
      toast.error('Please select a rating');
      return;
    }
    
    if (!formData.message.trim()) {
      toast.error('Please enter your feedback message');
      return;
    }
    
    setSubmitting(true);
    
    try {
      const token = localStorage.getItem('token');
      await axios.post(
        'http://localhost:8080/api/feedback',
        formData,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      
      toast.success('Thank you for your feedback!');
      onClose();
      
      // Reset form
      setFormData({
        feedbackType: 'SYSTEM',
        rating: 0,
        subject: '',
        message: ''
      });
    } catch (error) {
      toast.error('Failed to submit feedback');
      console.error(error);
    } finally {
      setSubmitting(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      {/* Backdrop */}
      <div 
        className="fixed inset-0 bg-black bg-opacity-50 transition-opacity" 
        onClick={onClose}
      ></div>
      
      {/* Modal */}
      <div className="flex min-h-full items-center justify-center p-4">
        <div className="relative bg-white rounded-xl shadow-xl max-w-2xl w-full">
          {/* Header */}
          <div className="flex items-center justify-between p-6 border-b border-gray-200">
            <div className="flex items-center space-x-3">
              <div className="bg-primary-100 p-2 rounded-lg">
                <MessageSquare className="h-6 w-6 text-primary-600" />
              </div>
              <h2 className="text-2xl font-bold text-gray-900">Share Your Feedback</h2>
            </div>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 transition-colors"
            >
              <X className="h-6 w-6" />
            </button>
          </div>

          {/* Body */}
          <form onSubmit={handleSubmit} className="p-6 space-y-6">
            {/* Feedback Type */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                What would you like to share?
              </label>
              <select
                value={formData.feedbackType}
                onChange={(e) => setFormData({...formData, feedbackType: e.target.value})}
                className="input-field"
                required
              >
                <option value="SYSTEM">System Performance</option>
                <option value="UI">User Interface</option>
                <option value="FEATURE_REQUEST">Feature Request</option>
                <option value="BUG_REPORT">Bug Report</option>
              </select>
            </div>

            {/* Rating */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                How would you rate your experience? *
              </label>
              <div className="flex space-x-2">
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setFormData({...formData, rating: star})}
                    onMouseEnter={() => setHoveredRating(star)}
                    onMouseLeave={() => setHoveredRating(0)}
                    className="focus:outline-none transition-transform hover:scale-110"
                  >
                    <Star
                      className={`h-10 w-10 transition-colors ${
                        star <= (hoveredRating || formData.rating)
                          ? 'fill-yellow-400 text-yellow-400'
                          : 'text-gray-300'
                      }`}
                    />
                  </button>
                ))}
              </div>
              {formData.rating > 0 && (
                <p className="text-sm text-gray-600 mt-2">
                  {formData.rating === 5 && '⭐ Excellent!'}
                  {formData.rating === 4 && '😊 Great!'}
                  {formData.rating === 3 && '👍 Good'}
                  {formData.rating === 2 && '😐 Fair'}
                  {formData.rating === 1 && '😞 Needs Improvement'}
                </p>
              )}
            </div>

            {/* Subject */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Subject
              </label>
              <input
                type="text"
                value={formData.subject}
                onChange={(e) => setFormData({...formData, subject: e.target.value})}
                className="input-field"
                placeholder="Brief description of your feedback"
                maxLength={255}
              />
            </div>

            {/* Message */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Your Feedback *
              </label>
              <textarea
                value={formData.message}
                onChange={(e) => setFormData({...formData, message: e.target.value})}
                className="input-field"
                rows="6"
                placeholder="Tell us more about your experience, suggestions, or issues you've encountered..."
                required
              />
              <p className="text-xs text-gray-500 mt-1">
                {formData.message.length} characters
              </p>
            </div>

            {/* Info Box */}
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
              <p className="text-sm text-blue-900">
                💡 Your feedback helps us improve! We review all submissions and may contact you for follow-up.
              </p>
            </div>

            {/* Buttons */}
            <div className="flex space-x-3">
              <button
                type="button"
                onClick={onClose}
                className="btn-secondary flex-1"
                disabled={submitting}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn-primary flex-1 flex items-center justify-center space-x-2"
                disabled={submitting}
              >
                {submitting ? (
                  <>
                    <div className="spinner w-4 h-4 border-2"></div>
                    <span>Submitting...</span>
                  </>
                ) : (
                  <>
                    <Send className="h-4 w-4" />
                    <span>Submit Feedback</span>
                  </>
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default FeedbackModal;