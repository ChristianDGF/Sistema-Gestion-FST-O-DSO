import http from 'k6/http';
import { check } from 'k6';
import { KEYCLOAK_URL, REALM, CLIENT_ID, TEST_USER } from '../config.js';

const TOKEN_URL = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`;

// Resource Owner Password Grant against the public client (directAccessGrantsEnabled=true
// for sistema-gestion-client, see keycloak/sistema-gestion-realm.json.template).
export function login(user = TEST_USER) {
  const res = http.post(
    TOKEN_URL,
    {
      grant_type: 'password',
      client_id: CLIENT_ID,
      username: user.username,
      password: user.password,
    },
    { tags: { name: 'keycloak_login' } }
  );

  check(res, { 'keycloak login succeeded': (r) => r.status === 200 });

  const body = res.json();
  return { accessToken: body.access_token, refreshToken: body.refresh_token };
}

// Access tokens are short-lived; long scenarios (load/stress) must refresh instead of
// re-sending credentials on every VU iteration.
export function refresh(refreshToken) {
  const res = http.post(
    TOKEN_URL,
    {
      grant_type: 'refresh_token',
      client_id: CLIENT_ID,
      refresh_token: refreshToken,
    },
    { tags: { name: 'keycloak_refresh' } }
  );

  if (res.status !== 200) {
    // Refresh token may have expired too (e.g. session idle timeout) - fall back to a fresh login.
    return login();
  }

  const body = res.json();
  return { accessToken: body.access_token, refreshToken: body.refresh_token };
}

export function authHeaders(accessToken) {
  return {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    },
  };
}
