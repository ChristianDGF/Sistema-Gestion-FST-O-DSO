import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import { Package, AlertTriangle, ArrowUpRight, ArrowDownRight } from 'lucide-react';

const Dashboard = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const response = await api.get('/dashboard');
        setData(response.data);
      } catch (error) {
        console.error('Failed to fetch dashboard data', error);
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

  const stats = [
    {
      name: 'Total Products',
      value: data?.totalProducts || 0,
      icon: Package,
      color: 'bg-blue-500',
    },
    {
      name: 'Low Stock Alerts',
      value: data?.lowStockProducts || 0,
      icon: AlertTriangle,
      color: 'bg-yellow-500',
    },
    {
      name: 'Total Value',
      value: `$${(data?.totalInventoryValue || 0).toLocaleString()}`,
      icon: ArrowUpRight,
      color: 'bg-green-500',
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-900">Dashboard Overview</h1>
      </div>

      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
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

      {/* Placeholder for charts or recent activity */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="card p-6">
          <h2 className="text-lg font-medium text-gray-900 mb-4">Inventory Status</h2>
          <div className="h-64 flex items-center justify-center bg-gray-50 rounded-lg border border-dashed border-gray-300">
            <span className="text-gray-500 text-sm">Chart Placeholder</span>
          </div>
        </div>
        <div className="card p-6">
          <h2 className="text-lg font-medium text-gray-900 mb-4">Recent Movements</h2>
          <div className="h-64 flex items-center justify-center bg-gray-50 rounded-lg border border-dashed border-gray-300">
            <span className="text-gray-500 text-sm">Activity Feed Placeholder</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
