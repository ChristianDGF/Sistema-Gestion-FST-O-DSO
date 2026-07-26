# Guía de Pruebas de Seguridad

Este documento cubre la sección **Security Testing** de la consigna del Proyecto Final
(Escaneo OWASP ZAP, Validación JWT, Validación de permisos, Validación de CORS,
OWASP Dependency Check / Snyk, Validación de autenticación), qué se implementó, cómo
ejecutarlo y qué defectos se encontraron y corrigieron en el proceso.

## 1. Dependency scanning (SCA)

| Componente | Herramienta | Comando | Reporte |
| --- | --- | --- | --- |
| Backend (JVM) | OWASP Dependency-Check (Gradle plugin) | `./gradlew dependencyCheckAnalyze` | `build/reports/dependency-check/dependency-check-report.html` |
| Frontend (npm) | `better-npm-audit` | `npm run audit` (dentro de `frontend/`) | salida de consola / `--json` |

- El backend falla el build si aparece una vulnerabilidad con CVSS >= 8. Los falsos
  positivos revisados se documentan con su justificación en
  [`dependency-check-suppressions.xml`](../dependency-check-suppressions.xml).
- El frontend usa `better-npm-audit` en vez de `npm audit` porque este último no permite
  aceptar una vulnerabilidad puntual sin desactivar todo el gate. Las excepciones
  documentadas están en [`frontend/.nsprc`](../frontend/.nsprc).
- `dependencyCheckAnalyze` requiere una `NVD_API_KEY` (secret de GitHub / variable de
  entorno) para no toparse con el rate limit del NVD; sin ella la actualización de la
  base de datos de CVEs puede tardar mucho o fallar.

### Defectos encontrados y corregidos

- **react-router-dom** tenía 4 vulnerabilidades high (open redirect, XSS, constructor
  injection, DoS de enrutamiento). Se actualizó a `7.18.1`. La única que queda
  (`GHSA-qwww-vcr4-c8h2`, CSRF en modo RSC) no tiene fix publicado aún en la línea 7.x
  y no aplica a esta SPA (usa `BrowserRouter`/`Routes` puro, sin RSC ni server actions)
  — documentada en `.nsprc`.
- **DOMPurify 3.2.4** (bundleado por swagger-ui vía `springdoc-openapi`) es vulnerable a
  `CVE-2025-48050` (DOM XSS, CVSS 7.5). Se actualizó `springdoc-openapi-starter-webmvc-ui`
  de `2.8.6` a `3.0.3`, que empaqueta una versión de swagger-ui con DOMPurify parcheado.

## 2. Validación JWT

[`JwtValidationTest`](../src/test/java/proyecto/sistemaGestion/security/JwtValidationTest.java)
ejercita el filtro real de `oauth2ResourceServer` (no `@WithMockUser`, que se salta el
parseo del JWT) firmando tokens con una clave RSA local
([`JwtTestSupport`](../src/test/java/proyecto/sistemaGestion/security/JwtTestSupport.java))
contra un `JwtDecoder` de prueba
([`JwtDecoderTestConfig`](../src/test/java/proyecto/sistemaGestion/security/JwtDecoderTestConfig.java)).

Casos cubiertos: sin header `Authorization` (401), token malformado (401), token
expirado (401), issuer incorrecto (401), firma no confiable (401), token válido sin
claim `permissions` (403), token válido con el scope correcto (200).

## 3. Validación de permisos

[`PermissionMatrixTest`](../src/test/java/proyecto/sistemaGestion/security/PermissionMatrixTest.java)
codifica la "Matriz mínima de permisos" de la consigna como test parametrizado: cada uno
de los 13 endpoints críticos (Productos, Stock, Reportes, Auditoría) se prueba con el
scope exacto requerido (éxito) y sin autoridades (403).

### Defecto encontrado y corregido

`AuditController` exigía `SCOPE_report:view` en vez de `SCOPE_audit:view`. El rol
`audit:view` ya existía como rol de Keycloak en `keycloak/sistema-gestion-realm.json`
pero nunca se aplicaba en código, permitiendo que cualquier usuario con acceso al
dashboard/reportes (`report:view`) leyera también el historial completo de auditoría.
Corregido; ver commit `fix(security): require audit:view scope on the audit trail
endpoints`.

## 4. Validación de CORS

[`CorsConfigurationTest`](../src/test/java/proyecto/sistemaGestion/security/CorsConfigurationTest.java)
envía preflight `OPTIONS` con distintos orígenes.

### Defecto encontrado y corregido

`CorsConfig` combinaba `setAllowedOriginPatterns(List.of("*"))` con
`setAllowCredentials(true)`, permitiendo que cualquier origen hiciera peticiones con
credenciales. Ahora los orígenes permitidos se leen de `APP_CORS_ALLOWED_ORIGINS`
(por defecto `http://localhost:5173`).

## 5. Escaneo OWASP ZAP (DAST)

Se ejecuta un [ZAP Baseline Scan](https://www.zaproxy.org/docs/docker/baseline-scan/)
(pasivo) contra el sistema ya desplegado vía `docker compose` — no contra artefactos de
build — cubriendo tanto el frontend (`http://localhost:5173`) como la superficie pública
del backend (Swagger UI, `http://localhost:8081/swagger-ui.html`).

Reglas/excepciones documentadas en [`.zap/rules.tsv`](../.zap/rules.tsv).

### Ejecutar localmente

```bash
docker compose up -d db keycloak
sleep 15
docker compose up -d app frontend
sleep 15
docker run --rm \
  -v "$(pwd)/.zap:/zap/wrk/config:ro" \
  -v "$(pwd)/zap-reports:/zap/wrk/:rw" \
  --network host ghcr.io/zaproxy/zaproxy:stable \
  zap-baseline.py -t http://localhost:5173 -r zap-frontend-report.html -c config/rules.tsv -a
```

### Defectos encontrados y corregidos

Escaneando el frontend recién desplegado (antes de cualquier cambio), ZAP reportó 11
warnings; tras las correcciones bajó a 2 (ambos documentados como riesgo aceptado, ver
abajo). Se agregó [`frontend/nginx.conf`](../frontend/nginx.conf) con:

- `X-Frame-Options: DENY` (faltaba — clickjacking)
- `X-Content-Type-Options: nosniff` (faltaba — MIME sniffing)
- `Content-Security-Policy` (no existía)
- `Permissions-Policy` (no existía)
- `Referrer-Policy: strict-origin-when-cross-origin`
- `server_tokens off` (el header `Server` filtraba la versión de nginx)

Verificado manualmente en navegador que el login OAuth2 (Keycloak) y las llamadas del
dashboard a la API siguen funcionando con la CSP activa.

Riesgo aceptado / diferido (quedan visibles como `WARN`, no suprimidos):

- **CSP `style-src 'unsafe-inline'`**: `src/pages/Audit.jsx` usa `style={{...}}` inline
  para las barras de progreso; quitarlo requeriría refactorizar a clases CSS
  dinámicas, fuera de alcance de este cambio.
- **`Cross-Origin-Embedder-Policy` ausente**: agregar COEP=`require-corp` puede romper
  la carga de recursos cross-origin (Keycloak, fuentes) sin una revisión más profunda
  de cada recurso cargado; se difiere.

Escaneando el backend (Swagger UI), ZAP detectó una **librería JS vulnerable**:
DOMPurify 3.2.4 (`CVE-2025-48050`, XSS DOM, CVSS 7.5) empaquetada dentro de
`swagger-ui-bundle.js` vía `springdoc-openapi`. Se actualizó
`springdoc-openapi-starter-webmvc-ui` de `2.8.6` a `3.0.3` (ver sección de
dependencias arriba), lo que sube el DOMPurify empaquetado a `3.3.2` y corrige
`CVE-2025-48050`.

**Riesgo residual (no falso positivo, pendiente de upstream):** DOMPurify `3.3.2`
sigue afectado por `CVE-2026-41238`, `CVE-2026-41239` y `CVE-2026-41240` (rango
vulnerable 3.0.1–3.3.3, fix real en `3.4.2`). Al momento de este cambio no existe
ninguna versión publicada de `springdoc-openapi-starter-webmvc-ui` que empaquete un
swagger-ui con DOMPurify `>= 3.4.0` — es la última disponible en Maven Central. Esta
alerta (`Vulnerable JS Library [10003]`) se deja **visible como WARN**, sin
suprimir en `.zap/rules.tsv`, y debe revisarse cuando springdoc publique una nueva
versión.

## 6. Integración en el pipeline

- **GitHub Actions** (`.github/workflows/ci.yml`): el job `build-and-test` corre
  `dependencyCheckAnalyze`, `npm run audit`, y el ZAP baseline scan contra el stack
  levantado con `docker compose` — es decir, contra el sistema desplegado, no solo
  durante el build de la imagen.
- **Jenkins** (`Jenkinsfile`): stage `Security Scan` equivalente, entre las pruebas E2E
  y el build de Docker.
