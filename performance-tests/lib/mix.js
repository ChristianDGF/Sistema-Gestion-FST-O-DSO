import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, TEST_USER } from '../config.js';
import { authHeaders } from './auth.js';

// Traffic mix modelled on real usage of ProductController/StockMovementController:
// listing/searching products dominates, writes are a smaller share.
const WEIGHTS = [
  { action: 'list_products', weight: 0.4 },
  { action: 'get_product', weight: 0.15 },
  { action: 'low_stock', weight: 0.1 },
  { action: 'stock_history', weight: 0.15 },
  { action: 'register_movement', weight: 0.15 },
  { action: 'create_product', weight: 0.05 },
];

export function pickAction() {
  const rand = Math.random();
  let cumulative = 0;
  for (const entry of WEIGHTS) {
    cumulative += entry.weight;
    if (rand <= cumulative) return entry.action;
  }
  return WEIGHTS[0].action;
}

function randomProductId(productIds) {
  return productIds[Math.floor(Math.random() * productIds.length)];
}

export function runAction(action, accessToken, productIds) {
  const auth = authHeaders(accessToken);
  const productId = randomProductId(productIds);

  switch (action) {
    case 'list_products': {
      const page = Math.floor(Math.random() * 5);
      const res = http.get(`${BASE_URL}/api/v1/products?page=${page}&size=20`, {
        ...auth,
        tags: { name: 'list_products' },
      });
      check(res, { 'list products: 200': (r) => r.status === 200 });
      return res;
    }
    case 'get_product': {
      const res = http.get(`${BASE_URL}/api/v1/products/${productId}`, {
        ...auth,
        tags: { name: 'get_product' },
      });
      check(res, { 'get product: 200': (r) => r.status === 200 });
      return res;
    }
    case 'low_stock': {
      const res = http.get(`${BASE_URL}/api/v1/products/low-stock?page=0&size=20`, {
        ...auth,
        tags: { name: 'low_stock' },
      });
      check(res, { 'low stock: 200': (r) => r.status === 200 });
      return res;
    }
    case 'stock_history': {
      const res = http.get(
        `${BASE_URL}/api/v1/stock-movements/product/${productId}?page=0&size=20`,
        { ...auth, tags: { name: 'stock_history' } }
      );
      check(res, { 'stock history: 200': (r) => r.status === 200 });
      return res;
    }
    case 'register_movement': {
      const movementType = Math.random() < 0.7 ? 'IN' : 'OUT';
      const res = http.post(
        `${BASE_URL}/api/v1/stock-movements`,
        JSON.stringify({
          productId,
          movementType,
          quantity: 1 + Math.floor(Math.random() * 3),
          userId: TEST_USER.username,
          observations: 'perf-test generated movement',
        }),
        { ...auth, tags: { name: 'register_movement' } }
      );
      check(res, { 'register movement: 201 or 400': (r) => r.status === 201 || r.status === 400 });
      return res;
    }
    case 'create_product': {
      const suffix = `${__VU}-${__ITER}-${Date.now()}`;
      const res = http.post(
        `${BASE_URL}/api/v1/products`,
        JSON.stringify({
          name: `Load Test Product ${suffix}`,
          sku: `LOAD-${suffix}`,
          description: 'Producto generado por performance-tests (load/stress)',
          category: 'Electronica',
          price: 19.99,
          quantity: 50,
          minStock: 5,
          status: 'ACTIVE',
        }),
        { ...auth, tags: { name: 'create_product' } }
      );
      const created = check(res, { 'create product: 201': (r) => r.status === 201 });
      // Delete right away so write load is exercised without growing the dataset unbounded
      // across long-running load/stress runs.
      if (created) {
        http.del(`${BASE_URL}/api/v1/products/${res.json('id')}`, null, {
          ...auth,
          tags: { name: 'cleanup_create_product' },
        });
      }
      return res;
    }
    default:
      return null;
  }
}
