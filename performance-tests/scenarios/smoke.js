import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL } from '../config.js';
import { login, authHeaders } from '../lib/auth.js';
import { seedProducts, teardownProducts } from '../lib/seed.js';

// Sanity check: 1 VU, 1 iteration through every target endpoint.
// Run this before load.js/stress.js against any environment to fail fast
// if something is misconfigured (wrong BASE_URL, Keycloak down, etc).
export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    http_req_failed: ['rate==0'],
    checks: ['rate==1'],
  },
};

export function setup() {
  const { accessToken } = login();
  const productIds = seedProducts(accessToken, 1);
  return { accessToken, productId: productIds[0] };
}

export default function (data) {
  const { accessToken, productId } = data;
  const auth = authHeaders(accessToken);

  let res = http.get(`${BASE_URL}/api/v1/products?page=0&size=20`, {
    ...auth,
    tags: { name: 'list_products' },
  });
  check(res, { 'list products: 200': (r) => r.status === 200 });

  res = http.get(`${BASE_URL}/api/v1/products/${productId}`, {
    ...auth,
    tags: { name: 'get_product' },
  });
  check(res, { 'get product: 200': (r) => r.status === 200 });

  res = http.get(`${BASE_URL}/api/v1/products/low-stock?page=0&size=20`, {
    ...auth,
    tags: { name: 'low_stock' },
  });
  check(res, { 'low stock: 200': (r) => r.status === 200 });

  res = http.get(`${BASE_URL}/api/v1/stock-movements/product/${productId}?page=0&size=20`, {
    ...auth,
    tags: { name: 'stock_history' },
  });
  check(res, { 'stock history: 200': (r) => r.status === 200 });

  res = http.post(
    `${BASE_URL}/api/v1/stock-movements`,
    JSON.stringify({
      productId,
      movementType: 'IN',
      quantity: 5,
      userId: 'employee',
      observations: 'smoke test movement',
    }),
    { ...auth, tags: { name: 'register_movement' } }
  );
  check(res, { 'register movement: 201': (r) => r.status === 201 });

  sleep(1);
}

export function teardown(data) {
  teardownProducts(data.accessToken, [data.productId]);
}
