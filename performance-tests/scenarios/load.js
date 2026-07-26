import { sleep } from 'k6';
import { BASELINE_THRESHOLDS } from '../config.js';
import { login } from '../lib/auth.js';
import { seedProducts, teardownProducts } from '../lib/seed.js';
import { pickAction, runAction } from '../lib/mix.js';
import { currentAccessToken } from '../lib/session.js';
import { buildSummary } from '../lib/report.js';

// Expected normal concurrency for a small/medium business: 20 concurrent users
// browsing/updating inventory for a sustained period.
export const options = {
  scenarios: {
    load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 20 }, // ramp-up
        { duration: '5m', target: 20 }, // sustained expected concurrency
        { duration: '1m', target: 0 }, // ramp-down
      ],
    },
  },
  thresholds: BASELINE_THRESHOLDS,
};

export function setup() {
  const { accessToken } = login();
  const productIds = seedProducts(accessToken, 500);
  return { productIds };
}

export default function (data) {
  const accessToken = currentAccessToken();
  runAction(pickAction(), accessToken, data.productIds);
  sleep(Math.random() * 2 + 1); // 1-3s think time between actions
}

export function teardown(data) {
  const { accessToken } = login();
  teardownProducts(accessToken, data.productIds);
}

export function handleSummary(data) {
  return buildSummary('load', data);
}
