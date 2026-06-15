import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import { Activity, ArrowDownToLine, ArrowUpFromLine, Search } from 'lucide-react';
import StockMovementModal from '../components/StockMovementModal';

const StockMovements = () => {
  const [products, setProducts] = useState([]);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [movements, setMovements] = useState([]);
  
  const [loadingProducts, setLoadingProducts] = useState(true);
  const [loadingMovements, setLoadingMovements] = useState(false);
  
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Fetch all products for the dropdown
  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const response = await api.get('/products', { params: { size: 100 } });
        setProducts(response.data.content || []);
      } catch (error) {
        console.error('Failed to fetch products', error);
      } finally {
        setLoadingProducts(false);
      }
    };
    fetchProducts();
  }, []);

  const fetchMovements = async (productId) => {
    if (!productId) {
      setMovements([]);
      return;
    }
    
    try {
      setLoadingMovements(true);
      const response = await api.get(`/stock-movements/product/${productId}/all`);
      setMovements(response.data || []);
    } catch (error) {
      console.error('Failed to fetch movements', error);
    } finally {
      setLoadingMovements(false);
    }
  };

  const handleProductChange = (e) => {
    const productId = e.target.value;
    const product = products.find(p => p.id === parseInt(productId, 10));
    setSelectedProduct(product || null);
    if (product) {
      fetchMovements(product.id);
    } else {
      setMovements([]);
    }
  };

  const handleRegisterMovement = () => {
    if (!selectedProduct) {
      alert("Please select a product first");
      return;
    }
    setIsModalOpen(true);
  };

  const onMovementSaved = () => {
    if (selectedProduct) {
      fetchMovements(selectedProduct.id);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <h1 className="text-2xl font-bold text-gray-900">Stock Movements</h1>
        <button 
          onClick={handleRegisterMovement} 
          disabled={!selectedProduct}
          className={`btn-primary flex items-center ${!selectedProduct ? 'opacity-50 cursor-not-allowed' : ''}`}
        >
          <Activity className="w-5 h-5 mr-2" />
          Register Movement
        </button>
      </div>

      <div className="card p-4">
        <div className="mb-6">
          <label className="block text-sm font-medium text-gray-700 mb-2">Select Product to view history</label>
          <div className="relative max-w-md">
            <select
              className="input-field appearance-none"
              onChange={handleProductChange}
              value={selectedProduct?.id || ''}
              disabled={loadingProducts}
            >
              <option value="">-- Choose a Product --</option>
              {products.map(p => (
                <option key={p.id} value={p.id}>
                  [{p.sku}] {p.name} (Current Stock: {p.quantity})
                </option>
              ))}
            </select>
          </div>
        </div>

        {selectedProduct ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Date</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Quantity</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Previous Stock</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">New Stock</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">User</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Observations</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {loadingMovements ? (
                  <tr>
                    <td colSpan="7" className="px-6 py-12 text-center">
                      <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
                    </td>
                  </tr>
                ) : movements.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="px-6 py-12 text-center text-gray-500">
                      No stock movements found for this product.
                    </td>
                  </tr>
                ) : (
                  movements.map((movement) => (
                    <tr key={movement.id} className="hover:bg-gray-50 transition-colors duration-150">
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {new Date(movement.createdAt).toLocaleString()}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                          movement.movementType === 'IN' ? 'bg-green-100 text-green-800' : 
                          movement.movementType === 'OUT' ? 'bg-red-100 text-red-800' : 
                          'bg-blue-100 text-blue-800'
                        }`}>
                          {movement.movementType === 'IN' && <ArrowDownToLine className="w-3 h-3 mr-1"/>}
                          {movement.movementType === 'OUT' && <ArrowUpFromLine className="w-3 h-3 mr-1"/>}
                          {movement.movementType === 'ADJUSTMENT' && <Activity className="w-3 h-3 mr-1"/>}
                          {movement.movementType}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        {movement.movementType === 'OUT' ? '-' : '+'}{movement.quantity}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {movement.previousQuantity}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        {movement.newQuantity}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {movement.userId}
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-500 max-w-xs truncate" title={movement.observations}>
                        {movement.observations || '-'}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="py-12 text-center text-gray-500 bg-gray-50 rounded-lg border border-dashed border-gray-300">
            <Activity className="w-12 h-12 text-gray-300 mx-auto mb-3" />
            <p>Select a product above to view its stock movement history</p>
          </div>
        )}
      </div>

      <StockMovementModal 
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        product={selectedProduct}
        onSave={onMovementSaved}
      />
    </div>
  );
};

export default StockMovements;
