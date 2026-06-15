import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import { X } from 'lucide-react';

const ProductModal = ({ isOpen, onClose, product, onSave }) => {
  const [formData, setFormData] = useState({
    name: '',
    sku: '',
    description: '',
    category: '',
    price: '',
    quantity: '',
    minStock: '',
    status: 'ACTIVE',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (product) {
      setFormData({
        name: product.name || '',
        sku: product.sku || '',
        description: product.description || '',
        category: product.category || '',
        price: product.price || '',
        quantity: product.quantity !== undefined ? product.quantity : '',
        minStock: product.minStock !== undefined ? product.minStock : '',
        status: product.status || 'ACTIVE',
      });
    } else {
      setFormData({
        name: '',
        sku: '',
        description: '',
        category: '',
        price: '',
        quantity: '',
        minStock: '',
        status: 'ACTIVE',
      });
    }
    setError(null);
  }, [product, isOpen]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    const payload = {
      ...formData,
      price: parseFloat(formData.price),
      quantity: parseInt(formData.quantity, 10),
      minStock: parseInt(formData.minStock, 10),
    };

    try {
      if (product && product.id) {
        await api.put(`/products/${product.id}`, payload);
      } else {
        await api.post('/products', payload);
      }
      onSave();
      onClose();
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || 'Error al guardar el producto');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4 overflow-y-auto">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-2xl overflow-hidden my-8">
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <h2 className="text-xl font-semibold text-gray-900">
            {product ? 'Edit Product' : 'Add New Product'}
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

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="col-span-1 md:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
              <input
                required
                type="text"
                name="name"
                value={formData.name}
                onChange={handleChange}
                className="input-field"
                placeholder="Product Name"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">SKU</label>
              <input
                required
                type="text"
                name="sku"
                value={formData.sku}
                onChange={handleChange}
                className="input-field"
                placeholder="PROD-001"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
              <input
                required
                type="text"
                name="category"
                value={formData.category}
                onChange={handleChange}
                className="input-field"
                placeholder="Electronics"
              />
            </div>

            <div className="col-span-1 md:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
              <textarea
                name="description"
                value={formData.description}
                onChange={handleChange}
                rows={3}
                className="input-field resize-none"
                placeholder="Optional description..."
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Price</label>
              <div className="relative">
                <span className="absolute left-3 top-2.5 text-gray-500">$</span>
                <input
                  required
                  type="number"
                  step="0.01"
                  min="0.01"
                  name="price"
                  value={formData.price}
                  onChange={handleChange}
                  className="input-field pl-8"
                  placeholder="0.00"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
              <select
                name="status"
                value={formData.status}
                onChange={handleChange}
                className="input-field bg-white"
              >
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Initial Quantity</label>
              <input
                required
                type="number"
                min="0"
                name="quantity"
                value={formData.quantity}
                onChange={handleChange}
                className="input-field"
                placeholder="0"
                disabled={!!product} // Disable quantity editing on edit, usually managed by stock movements
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Min Stock Alert</label>
              <input
                required
                type="number"
                min="0"
                name="minStock"
                value={formData.minStock}
                onChange={handleChange}
                className="input-field"
                placeholder="5"
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
              {product ? 'Save Changes' : 'Create Product'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ProductModal;
