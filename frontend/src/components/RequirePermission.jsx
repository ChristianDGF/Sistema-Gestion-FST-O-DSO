import React from 'react';
import { useKeycloak } from '../auth/KeycloakContext';
import AccessDenied from './AccessDenied';

const RequirePermission = ({ permission, children }) => {
  const { hasPermission } = useKeycloak();

  if (!hasPermission(permission)) {
    return <AccessDenied requiredPermission={permission} />;
  }

  return children;
};

export default RequirePermission;
