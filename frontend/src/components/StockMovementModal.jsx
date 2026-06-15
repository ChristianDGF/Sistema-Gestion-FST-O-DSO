import React, { useState } from 'react';
import api from '../api/axios';
import { X } from 'lucide-react';
import { useKeycloak } from '../auth/KeycloakContext';

const StockMovementModal = ({ isOpen, onClose, product, onSave }) => {
  const { user } = useKeycloak();
  const [formData, setFormData] = useState({
    movementType: 'IN',
    quantity: '',
    observations: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    if (!product) {
      setError('A product must be selected.');
      setLoading(false);
      return;
    }

    const payload = {
      productId: product.id,
      movementType: formData.movementType,
      quantity: parseInt(formData.quantity, 10),
      observations: formData.observations,
      userId: user?.username || 'unknown',
    };

    try {
      await api.post('/stock-movements', payload);
      onSave();
      onClose();
      // reset form
      setFormData({
        movementType: 'IN',
        quantity: '',
        observations: '',
      });
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || 'Error registering stock movement');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg overflow-hidden">
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <h2 className="text-xl font-semibold text-gray-900">
            Register Movement - {product?.name}
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6">
          {error && (
            <div className="mb-4 p-3 bg-red-50 text-red-700 rounded-lg text-sm">
              {error}
            </div>
          )}

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Type</label>
              <select
                name="movementType"
                value={formData.movementType}
                onChange={handleChange}
                className="input-field bg-white"
              >
                <option value="IN">Stock Entry (IN)</option>
                <option value="OUT">Stock Dispatch (OUT)</option>
                <option value="ADJUSTMENT">Stock Adjustment</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Quantity</label>
              <input
                required
                type="number"
                min="1"
                name="quantity"
                value={formData.quantity}
                onChange={handleChange}
                className="input-field"
                placeholder="Amount to move"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Observations</label>
              <textarea
                name="observations"
                value={formData.observations}
                onChange={handleChange}
                rows={3}
                className="input-field resize-none"
                placeholder="Reason for movement, reference numbers..."
              />
            </div>
          </div>

          <div className="mt-8 flex justify-end space-x-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="btn-primary flex items-center"
            >
              {loading ? (
                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin mr-2"></div>
              ) : null}
              Save Movement
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default StockMovementModal;
