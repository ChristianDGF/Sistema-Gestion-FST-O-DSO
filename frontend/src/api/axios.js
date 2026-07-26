import axios from 'axios';
import keycloak from '../auth/keycloak';

const api = axios.create({
  baseURL: 'http://localhost:8081/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    if (keycloak.token) {
      config.headers['Authorization'] = `Bearer ${keycloak.token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      keycloak.login();
    } else if (error.response && error.response.status === 403) {
      console.warn(`Forbidden: missing permission for ${error.config?.method?.toUpperCase()} ${error.config?.url}`);
    }
    return Promise.reject(error);
  }
);

export default api;
