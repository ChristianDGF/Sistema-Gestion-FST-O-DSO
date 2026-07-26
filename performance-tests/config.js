// Shared configuration for all k6 scenarios.
// Override via environment variables, e.g.: k6 run -e BASE_URL=https://staging.example.com scenarios/load.js

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
export const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://localhost:8080';
export const REALM = __ENV.KEYCLOAK_REALM || 'sistema-gestion';
export const CLIENT_ID = __ENV.KEYCLOAK_CLIENT_ID || 'sistema-gestion-client';

export const TEST_USER = {
  username: __ENV.PERF_TEST_USERNAME || 'employee',
  password: __ENV.PERF_TEST_PASSWORD || 'employee123',
};

// Number of iterations a VU runs before it re-authenticates against Keycloak.
// Access tokens are short-lived, so long-running load/stress scenarios must refresh mid-run.
export const TOKEN_REFRESH_EVERY_ITERATIONS = 25;

// Thresholds shared by scenarios that expect the system to stay healthy under load.
// stress.js intentionally relaxes http_req_failed since degradation past a point is expected.
export const BASELINE_THRESHOLDS = {
  http_req_duration: ['p(95)<800', 'p(99)<1500'],
  'http_req_duration{name:list_products}': ['p(95)<600'],
  'http_req_duration{name:get_product}': ['p(95)<400'],
  'http_req_duration{name:low_stock}': ['p(95)<600'],
  'http_req_duration{name:stock_history}': ['p(95)<600'],
  'http_req_duration{name:register_movement}': ['p(95)<800'],
  'http_req_duration{name:create_product}': ['p(95)<800'],
  http_req_failed: ['rate<0.01'],
  checks: ['rate>0.99'],
};
