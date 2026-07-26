import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import {
  Package, AlertTriangle, CheckCircle, Activity,
  ArrowDownToLine, ArrowUpFromLine, AlertCircle,
} from 'lucide-react';
import BarGroup from '../components/charts/BarGroup';
import EmptyState from '../components/EmptyState';

const MOVEMENT_ICON = {
  IN: ArrowDownToLine,
  OUT: ArrowUpFromLine,
  ADJUSTMENT: Activity,
};

const Dashboard = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        setError(null);
        const response = await api.get('/dashboard');
        setData(response.data);
      } catch (error) {
        console.error('Failed to fetch dashboard data', error);
        setError('No se pudieron cargar los datos del dashboard.');
      } finally {
        setLoading(false);
      }
    };
    fetchDashboard();
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center gap-2 text-red-500 card p-6">
        <AlertCircle className="w-5 h-5" /> {error}
      </div>
    );
  }

  const stats = [
    {
      name: 'Total Products',
      value: data?.totalProducts || 0,
      icon: Package,
      color: 'bg-blue-500',
    },
    {
      name: 'Active Products',
      value: data?.activeProducts || 0,
      icon: CheckCircle,
      color: 'bg-green-500',
    },
    {
      name: 'Low Stock Alerts',
      value: data?.lowStockProducts || 0,
      icon: AlertTriangle,
      color: 'bg-yellow-500',
    },
    {
      name: 'Total Movements',
      value: data?.totalMovements || 0,
      icon: Activity,
      color: 'bg-purple-500',
    },
  ];

  const criticalProducts = data?.criticalProducts || [];
  const maxCriticalQuantity = Math.max(...criticalProducts.map((p) => p.quantity ?? 0), 1);
  const recentMovements = data?.recentMovements || [];

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-900">Dashboard Overview</h1>
      </div>

      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((stat) => (
          <div key={stat.name} className="card p-6 flex items-center">
            <div className={`p-4 rounded-full ${stat.color} bg-opacity-10 mr-4`}>
              <stat.icon className={`w-8 h-8 ${stat.color.replace('bg-', 'text-')}`} />
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">{stat.name}</p>
              <p className="text-3xl font-bold text-gray-900">{stat.value}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="card p-6">
          <h2 className="text-lg font-medium text-gray-900 mb-4">Inventory Status</h2>
          {criticalProducts.length === 0 ? (
            <div className="h-64 flex items-center justify-center">
              <EmptyState message="No hay productos con stock bajo" />
            </div>
          ) : (
            <div className="h-64 overflow-y-auto space-y-3 pr-1">
              {criticalProducts.map((product) => (
                <BarGroup
                  key={product.id}
                  label={product.name}
                  value={product.quantity}
                  max={maxCriticalQuantity}
                  color={product.quantity === 0 ? 'bg-red-500' : 'bg-amber-400'}
                />
              ))}
            </div>
          )}
        </div>
        <div className="card p-6">
          <h2 className="text-lg font-medium text-gray-900 mb-4">Recent Movements</h2>
          {recentMovements.length === 0 ? (
            <div className="h-64 flex items-center justify-center">
              <EmptyState message="Sin movimientos de stock registrados" />
            </div>
          ) : (
            <div className="h-64 overflow-y-auto space-y-1 pr-1">
              {recentMovements.map((movement) => {
                const Icon = MOVEMENT_ICON[movement.movementType] || Activity;
                const isOut = movement.movementType === 'OUT';
                return (
                  <div
                    key={movement.id}
                    className="flex items-center gap-3 py-2 border-b border-gray-100 last:border-0"
                  >
                    <div className={`p-2 rounded-full ${isOut ? 'bg-red-50' : 'bg-green-50'}`}>
                      <Icon className={`w-4 h-4 ${isOut ? 'text-red-600' : 'text-green-600'}`} />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900 truncate">{movement.productName}</p>
                      <p className="text-xs text-gray-400 truncate">
                        {new Date(movement.createdAt).toLocaleString()}
                        {movement.observations ? ` · ${movement.observations}` : ''}
                      </p>
                    </div>
                    <span className={`text-sm font-semibold shrink-0 ${isOut ? 'text-red-600' : 'text-green-600'}`}>
                      {isOut ? '-' : '+'}{movement.quantity}
                    </span>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
