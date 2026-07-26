import { TOKEN_REFRESH_EVERY_ITERATIONS } from '../config.js';
import { login, refresh } from './auth.js';

// k6 gives each VU its own module instances, so these module-level variables
// are effectively per-VU state that persists across iterations of the same VU.
let vuToken = null;
let vuIterations = 0;

// Returns a valid access token for the current VU, logging in on the first
// call and refreshing periodically since access tokens outlive a single
// iteration but not a full load/stress/spike run.
export function currentAccessToken() {
  if (!vuToken) {
    vuToken = login();
  } else if (vuIterations > 0 && vuIterations % TOKEN_REFRESH_EVERY_ITERATIONS === 0) {
    vuToken = refresh(vuToken.refreshToken);
  }
  vuIterations++;
  return vuToken.accessToken;
}
