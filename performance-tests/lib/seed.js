import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from '../config.js';
import { authHeaders } from './auth.js';

const CATEGORIES = ['Electronica', 'Oficina', 'Limpieza', 'Ferreteria', 'Alimentos'];

function randomProductPayload(index) {
  return JSON.stringify({
    name: `Perf Test Product ${index}`,
    sku: `PERF-${__VU}-${index}-${Date.now()}`,
    description: 'Producto generado por performance-tests/lib/seed.js',
    category: CATEGORIES[index % CATEGORIES.length],
    price: 9.99 + (index % 50),
    quantity: 100 + (index % 20),
    minStock: 10,
    status: 'ACTIVE',
  });
}

// Creates `count` products via the real API so load/stress scenarios have realistic
// volume to paginate/search/filter over. Returns the created product IDs.
export function seedProducts(accessToken, count) {
  const ids = [];
  for (let i = 0; i < count; i++) {
    const res = http.post(`${BASE_URL}/api/v1/products`, randomProductPayload(i), {
      ...authHeaders(accessToken),
      tags: { name: 'seed_create_product' },
    });

    const ok = check(res, { 'seed product created': (r) => r.status === 201 });
    if (ok) {
      ids.push(res.json('id'));
    }
  }
  return ids;
}

// Cleans up everything created by seedProducts so staging stays in a repeatable state
// between performance test runs. Best-effort: the API rejects deleting a product that
// has stock movements associated with it (by design, for audit/traceability), and
// scenarios do register movements against seeded products during the run - so a 400
// here is an expected outcome, not a failure, and must not fail http_req_failed.
export function teardownProducts(accessToken, ids) {
  ids.forEach((id) => {
    http.del(`${BASE_URL}/api/v1/products/${id}`, null, {
      ...authHeaders(accessToken),
      tags: { name: 'seed_delete_product' },
      responseCallback: http.expectedStatuses(204, 400),
    });
  });
}
