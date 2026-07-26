import { sleep } from 'k6';
import { login } from '../lib/auth.js';
import { seedProducts, teardownProducts } from '../lib/seed.js';
import { pickAction, runAction } from '../lib/mix.js';
import { currentAccessToken } from '../lib/session.js';

// Optional scenario: sudden jump from low to high concurrency (e.g. a month-end
// closing rush) to validate recovery behaviour rather than sustained capacity.
export const options = {
  scenarios: {
    spike: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 5 },
        { duration: '10s', target: 150 }, // abrupt spike
        { duration: '1m', target: 150 },
        { duration: '20s', target: 5 }, // recovery
        { duration: '30s', target: 5 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.2'],
  },
};

export function setup() {
  const { accessToken } = login();
  const productIds = seedProducts(accessToken, 200);
  return { productIds };
}

export default function (data) {
  const accessToken = currentAccessToken();
  runAction(pickAction(), accessToken, data.productIds);
  sleep(Math.random() + 0.5);
}

export function teardown(data) {
  const { accessToken } = login();
  teardownProducts(accessToken, data.productIds);
}
