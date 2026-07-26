import { sleep } from 'k6';
import { login } from '../lib/auth.js';
import { seedProducts, teardownProducts } from '../lib/seed.js';
import { pickAction, runAction } from '../lib/mix.js';
import { currentAccessToken } from '../lib/session.js';
import { buildSummary } from '../lib/report.js';

// Escalating stages well beyond the expected 20 concurrent users (see load.js) to find
// the point where the system degrades. http_req_failed is intentionally NOT capped at a
// strict rate here - some failure once the system is overloaded is the expected outcome;
// the goal is to observe/report at which VU count p95 latency or the error rate spike,
// not to pass/fail the build on it.
export const options = {
  scenarios: {
    stress: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 50 },
        { duration: '2m', target: 100 },
        { duration: '2m', target: 200 },
        { duration: '2m', target: 300 },
        { duration: '2m', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<3000'],
    http_req_failed: ['rate<0.5'],
  },
};

export function setup() {
  const { accessToken } = login();
  const productIds = seedProducts(accessToken, 500);
  return { productIds };
}

export default function (data) {
  const accessToken = currentAccessToken();
  runAction(pickAction(), accessToken, data.productIds);
  sleep(Math.random() + 0.5); // shorter think time to push more throughput per VU
}

export function teardown(data) {
  const { accessToken } = login();
  teardownProducts(accessToken, data.productIds);
}

export function handleSummary(data) {
  return buildSummary('stress', data);
}
