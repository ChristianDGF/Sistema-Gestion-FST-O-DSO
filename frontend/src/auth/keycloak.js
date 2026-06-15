import Keycloak from 'keycloak-js';

const keycloakConfig = {
  url: 'http://localhost:8080',
  realm: 'sistema-gestion',
  clientId: 'sistema-gestion-client'
};

const keycloak = new Keycloak(keycloakConfig);

export default keycloak;
