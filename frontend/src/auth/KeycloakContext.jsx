import React, { createContext, useContext, useEffect, useState } from 'react';
import keycloak from './keycloak';

const KeycloakContext = createContext({
  isAuthenticated: false,
  token: null,
  login: () => {},
  logout: () => {},
  user: null,
  permissions: [],
  hasPermission: () => false,
  hasAnyPermission: () => false,
});

export const KeycloakProvider = ({ children }) => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [token, setToken] = useState(null);
  const [user, setUser] = useState(null);
  const [permissions, setPermissions] = useState([]);
  const [isInitialized, setIsInitialized] = useState(false);
  const isRun = React.useRef(false);

  useEffect(() => {
    if (isRun.current) return;
    isRun.current = true;

    keycloak.init({ onLoad: 'login-required', checkLoginIframe: false })
      .then((authenticated) => {
        setIsAuthenticated(authenticated);
        if (authenticated) {
          setToken(keycloak.token);
          // Use tokenParsed instead of loadUserProfile to avoid CORS
          setUser({
            firstName: keycloak.tokenParsed?.given_name,
            lastName: keycloak.tokenParsed?.family_name,
            username: keycloak.tokenParsed?.preferred_username,
            email: keycloak.tokenParsed?.email,
          });
          setPermissions(keycloak.tokenParsed?.permissions || []);
        }
        setIsInitialized(true);
      })
      .catch((error) => {
        console.error('Keycloak initialization failed', error);
        setIsInitialized(true);
      });

    keycloak.onTokenExpired = () => {
      keycloak.updateToken(30).then((refreshed) => {
        if (refreshed) {
          setToken(keycloak.token);
          setPermissions(keycloak.tokenParsed?.permissions || []);
        }
      }).catch(() => {
        console.error('Failed to refresh token');
        keycloak.logout();
      });
    };
  }, []);

  const hasPermission = (scope) => permissions.includes(scope);
  const hasAnyPermission = (scopes) => scopes.some(hasPermission);

  if (!isInitialized) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  return (
    <KeycloakContext.Provider
      value={{
        isAuthenticated,
        token,
        login: () => keycloak.login(),
        logout: () => keycloak.logout(),
        user,
        permissions,
        hasPermission,
        hasAnyPermission,
      }}
    >
      {children}
    </KeycloakContext.Provider>
  );
};

export const useKeycloak = () => useContext(KeycloakContext);
