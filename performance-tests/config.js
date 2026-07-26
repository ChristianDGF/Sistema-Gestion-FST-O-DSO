// Shared configuration for all k6 scenarios.
// Override via environment variables, e.g.: k6 run -e BASE_URL=https://staging.example.com scenarios/load.js

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
export const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://localhost:8080';
export const REALM = __ENV.KEYCLOAK_REALM || 'sistema-gestion';
export const CLIENT_ID = __ENV.KEYCLOAK_CLIENT_ID || 'sistema-gestion-client';

// Where handleSummary() writes HTML/JSON reports. k6 resolves this relative to its own
// working directory (not the script's location), which differs by invocation context:
// running "k6 run performance-tests/scenarios/x.js" from the repo root vs. running
// inside the grafana/k6 Docker image with the folder mounted at /scripts. Override with
// REPORTS_DIR when the working directory isn't the repo root (see Jenkinsfile).
export const REPORTS_DIR = __ENV.REPORTS_DIR || 'performance-tests/reports';

// Realm roles (keycloak/sistema-gestion-realm.json) are granular: "employee" only
// carries product:view/stock:view. The traffic mix in lib/mix.js also exercises
// product:manage/stock:manage (create product, register movement), so the default
// test identity must be "admin", which holds every permission.
export const TEST_USER = {
  username: __ENV.PERF_TEST_USERNAME || 'admin',
  password: __ENV.PERF_TEST_PASSWORD || 'admin123',
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
