import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { LayoutDashboard, Package, LogOut, Activity, ShieldCheck } from 'lucide-react';
import { useKeycloak } from '../auth/KeycloakContext';
import clsx from 'clsx';

const Sidebar = () => {
  const { pathname } = useLocation();
  const { logout } = useKeycloak();

  const navigation = [
    { name: 'Dashboard', href: '/', icon: LayoutDashboard },
    { name: 'Products', href: '/products', icon: Package },
    { name: 'Stock Movements', href: '/stock-movements', icon: Activity },
    { name: 'Auditoría', href: '/audit', icon: ShieldCheck },
  ];

  return (
    <div className="flex flex-col w-64 h-screen bg-white border-r border-gray-200">
      <div className="flex items-center justify-center h-16 border-b border-gray-200">
        <span className="text-xl font-bold text-primary-600">Inventory Sys</span>
      </div>
      <div className="flex-1 overflow-y-auto">
        <nav className="px-4 mt-6 space-y-2">
          {navigation.map((item) => {
            const isActive = pathname === item.href || (item.href !== '/' && pathname.startsWith(item.href));
            return (
              <Link
                key={item.name}
                to={item.href}
                className={clsx(
                  'flex items-center px-4 py-3 text-sm font-medium rounded-lg transition-colors duration-200',
                  isActive
                    ? 'bg-primary-50 text-primary-700'
                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                )}
              >
                <item.icon
                  className={clsx('w-5 h-5 mr-3', isActive ? 'text-primary-600' : 'text-gray-400')}
                />
                {item.name}
              </Link>
            );
          })}
        </nav>
      </div>
      <div className="p-4 border-t border-gray-200">
        <button
          onClick={logout}
          className="flex items-center w-full px-4 py-3 text-sm font-medium text-red-600 rounded-lg hover:bg-red-50 transition-colors duration-200"
        >
          <LogOut className="w-5 h-5 mr-3" />
          Logout
        </button>
      </div>
    </div>
  );
};

export default Sidebar;
