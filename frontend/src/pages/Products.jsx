import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import { Plus, Search, Filter, Edit, Trash2, ChevronUp, ChevronDown, X } from 'lucide-react';
import ProductModal from '../components/ProductModal';
import Pagination from '../components/Pagination';
import { useKeycloak } from '../auth/KeycloakContext';
import { PERMISSIONS } from '../config/permissions';

const PAGE_SIZE = 20;

const SORTABLE_COLUMNS = [
  { key: 'name', label: 'Product' },
  { key: 'category', label: 'Category' },
  { key: 'price', label: 'Price' },
  { key: 'quantity', label: 'Stock' },
  { key: 'status', label: 'Status' },
];

const SortableHeader = ({ column, sortBy, sortDir, onSort }) => {
  const isActive = sortBy === column.key;
  return (
    <th
      onClick={() => onSort(column.key)}
      className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer select-none hover:text-gray-700"
    >
      <span className="inline-flex items-center gap-1">
        {column.label}
        {isActive && (sortDir === 'asc' ? <ChevronUp className="w-3 h-3" /> : <ChevronDown className="w-3 h-3" />)}
      </span>
    </th>
  );
};

const Products = () => {
  const { hasPermission } = useKeycloak();
  const canManageProducts = hasPermission(PERMISSIONS.PRODUCT_MANAGE);

  const [products, setProducts] = useState([]);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);

  const [searchInput, setSearchInput] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  const [page, setPage] = useState(0);
  const [sortBy, setSortBy] = useState('name');
  const [sortDir, setSortDir] = useState('asc');

  const [showFilters, setShowFilters] = useState(false);
  const [category, setCategory] = useState('');
  const [status, setStatus] = useState('');
  const [lowStockOnly, setLowStockOnly] = useState(false);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState(null);

  // Debounce free-text search input
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(searchInput), 400);
    return () => clearTimeout(timer);
  }, [searchInput]);

  // Reset to first page whenever a filter/search changes
  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, category, status, lowStockOnly]);

  const fetchProducts = async () => {
    try {
      setLoading(true);
      const response = await api.get('/products', {
        params: {
          page,
          size: PAGE_SIZE,
          sortBy,
          sortDir,
          search: debouncedSearch || undefined,
          category: category || undefined,
          status: status || undefined,
          lowStock: lowStockOnly || undefined,
        },
      });
      setProducts(response.data.content || []);
      setTotalPages(response.data.totalPages ?? 1);
    } catch (error) {
      console.error('Failed to fetch products', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, sortBy, sortDir, debouncedSearch, category, status, lowStockOnly]);

  const handleSort = (columnKey) => {
    if (sortBy === columnKey) {
      setSortDir((prev) => (prev === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortBy(columnKey);
      setSortDir('asc');
    }
  };

  const handleClearFilters = () => {
    setCategory('');
    setStatus('');
    setLowStockOnly(false);
  };

  const hasActiveFilters = category || status || lowStockOnly;

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this product?')) {
      try {
        await api.delete(`/products/${id}`);
        fetchProducts();
      } catch (error) {
        const message = error.response?.data?.message || 'Could not delete product. It may have stock movements associated.';
        alert(message);
      }
    }
  };

  const handleAddClick = () => {
    setSelectedProduct(null);
    setIsModalOpen(true);
  };

  const handleEditClick = (product) => {
    setSelectedProduct(product);
    setIsModalOpen(true);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <h1 className="text-2xl font-bold text-gray-900">Products Management</h1>
        {canManageProducts && (
          <button onClick={handleAddClick} className="btn-primary flex items-center">
            <Plus className="w-5 h-5 mr-2" />
            Add Product
          </button>
        )}
      </div>

      <div className="card p-4">
        <div className="flex flex-col sm:flex-row justify-between items-center gap-4 mb-4">
          <div className="relative w-full sm:w-96">
            <input
              type="text"
              placeholder="Search products..."
              className="input-field pl-10"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
            />
            <Search className="w-5 h-5 text-gray-400 absolute left-3 top-2.5" />
          </div>
          <button
            onClick={() => setShowFilters((prev) => !prev)}
            className={`flex items-center px-4 py-2 text-sm font-medium border rounded-lg transition-colors ${
              showFilters || hasActiveFilters
                ? 'text-primary-700 bg-primary-50 border-primary-200'
                : 'text-gray-700 bg-white border-gray-300 hover:bg-gray-50'
            }`}
          >
            <Filter className="w-4 h-4 mr-2" />
            Filters
            {hasActiveFilters && (
              <span className="ml-2 inline-flex items-center justify-center w-5 h-5 rounded-full bg-primary-600 text-white text-xs">
                {[category, status, lowStockOnly].filter(Boolean).length}
              </span>
            )}
          </button>
        </div>

        {showFilters && (
          <div className="flex flex-col sm:flex-row items-start sm:items-center gap-3 mb-4 p-4 bg-gray-50 rounded-lg border border-gray-200">
            <input
              type="text"
              placeholder="Category..."
              className="input-field sm:w-48"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
            />
            <select
              className="input-field sm:w-40"
              value={status}
              onChange={(e) => setStatus(e.target.value)}
            >
              <option value="">All statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </select>
            <label className="flex items-center gap-2 text-sm text-gray-700 whitespace-nowrap">
              <input
                type="checkbox"
                checked={lowStockOnly}
                onChange={(e) => setLowStockOnly(e.target.checked)}
                className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
              />
              Low stock only
            </label>
            {hasActiveFilters && (
              <button
                onClick={handleClearFilters}
                className="flex items-center text-sm text-gray-500 hover:text-gray-700"
              >
                <X className="w-4 h-4 mr-1" />
                Clear filters
              </button>
            )}
          </div>
        )}

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                {SORTABLE_COLUMNS.map((column) => (
                  <SortableHeader
                    key={column.key}
                    column={column}
                    sortBy={sortBy}
                    sortDir={sortDir}
                    onSort={handleSort}
                  />
                ))}
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">SKU</th>
                {canManageProducts && (
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                )}
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {loading ? (
                <tr>
                  <td colSpan={canManageProducts ? 7 : 6} className="px-6 py-12 text-center">
                    <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
                  </td>
                </tr>
              ) : products.length === 0 ? (
                <tr>
                  <td colSpan={canManageProducts ? 7 : 6} className="px-6 py-12 text-center text-gray-500">
                    No products found
                  </td>
                </tr>
              ) : (
                products.map((product) => (
                  <tr key={product.id} className="hover:bg-gray-50 transition-colors duration-150">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">{product.name}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{product.category}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 font-medium">
                      ${product.price?.toLocaleString()}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center">
                        <span className={`text-sm ${product.lowStock ? 'text-red-600 font-medium' : 'text-gray-900'}`}>
                          {product.quantity}
                        </span>
                        {product.lowStock && (
                          <span className="ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-red-100 text-red-800">
                            Low
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        product.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                      }`}>
                        {product.status || 'ACTIVE'}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{product.sku}</td>
                    {canManageProducts && (
                      <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                        <button
                          onClick={() => handleEditClick(product)}
                          className="text-primary-600 hover:text-primary-900 mr-4 transition-colors"
                        >
                          <Edit className="w-5 h-5" />
                        </button>
                        <button
                          className="text-red-600 hover:text-red-900 transition-colors"
                          onClick={() => handleDelete(product.id)}
                        >
                          <Trash2 className="w-5 h-5" />
                        </button>
                      </td>
                    )}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {!loading && products.length > 0 && (
          <Pagination page={page} totalPages={totalPages} onPage={setPage} />
        )}
      </div>

      <ProductModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        product={selectedProduct}
        onSave={fetchProducts}
      />
    </div>
  );
};

export default Products;
