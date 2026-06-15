import React from 'react';
import { Bell, User } from 'lucide-react';
import { useKeycloak } from '../auth/KeycloakContext';

const Navbar = () => {
  const { user } = useKeycloak();

  return (
    <header className="flex items-center justify-between h-16 px-6 bg-white border-b border-gray-200">
      <div className="flex items-center">
        {/* Breadcrumbs or page title could go here */}
      </div>
      <div className="flex items-center space-x-4">
        <button className="p-2 text-gray-400 hover:text-gray-500 hover:bg-gray-100 rounded-full transition-colors duration-200">
          <Bell className="w-5 h-5" />
        </button>
        <div className="flex items-center space-x-3">
          <div className="flex flex-col items-end hidden sm:flex">
            <span className="text-sm font-medium text-gray-900">
              {user?.firstName} {user?.lastName}
            </span>
            <span className="text-xs text-gray-500">{user?.username}</span>
          </div>
          <div className="flex items-center justify-center w-10 h-10 bg-primary-100 rounded-full">
            <User className="w-5 h-5 text-primary-600" />
          </div>
        </div>
      </div>
    </header>
  );
};

export default Navbar;
