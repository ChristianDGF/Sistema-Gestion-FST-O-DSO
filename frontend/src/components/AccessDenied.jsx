import React from 'react';
import { Link } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';
import { useKeycloak } from '../auth/KeycloakContext';
import { ROUTE_PERMISSIONS } from '../config/permissions';

const AccessDenied = ({ requiredPermission }) => {
  const { hasPermission } = useKeycloak();
  const fallbackRoute = Object.entries(ROUTE_PERMISSIONS)
    .find(([, permission]) => hasPermission(permission))?.[0];

  return (
    <div className="flex flex-col items-center justify-center py-24 text-center card">
      <div className="p-4 rounded-full bg-red-50 mb-4">
        <ShieldAlert className="w-10 h-10 text-red-500" />
      </div>
      <h1 className="text-xl font-bold text-gray-900 mb-2">Acceso denegado</h1>
      <p className="text-sm text-gray-500 max-w-md">
        No tienes el permiso necesario para ver esta página
        {requiredPermission && (
          <> (<code className="px-1.5 py-0.5 bg-gray-100 rounded text-xs">{requiredPermission}</code>)</>
        )}.
      </p>
      {fallbackRoute && (
        <Link to={fallbackRoute} className="btn-primary mt-6">
          Volver al inicio
        </Link>
      )}
    </div>
  );
};

export default AccessDenied;
