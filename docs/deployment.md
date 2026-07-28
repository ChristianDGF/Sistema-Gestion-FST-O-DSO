# Despliegue: Staging y Production

Documento técnico de cómo quedaron desplegados los ambientes de Staging y Production, cómo se configuraron las VMs de GCP, y cómo funciona el pipeline de GitHub Actions que los mantiene actualizados. Complementa a `cicd_implementation_plan.md` (que tiene el historial completo de decisiones y bugs encontrados durante la implementación) con una explicación de referencia del estado final.

## 1. Arquitectura general

```
feature/*  →  development  →  staging  →  main
                (integración)   (VM Staging)    (VM Production)
```

Cada rama de promoción tiene su propia VM en GCP, cada una corriendo el stack completo de `docker-compose.yml` (backend, frontend, Postgres, Keycloak, y todo el stack de observabilidad) más un reverse proxy Nginx con TLS delante.

| | Staging | Production |
|---|---|---|
| VM (GCP, `us-central1-a`) | `sistema-gestion-staging` | `sistema-gestion-prod` |
| IP externa estática | `34.136.168.229` | `34.57.191.185` |
| Rama que dispara el deploy | `staging` | `main` |
| Frontend | `https://staging.34-136-168-229.nip.io` | `https://production.34-57-191-185.nip.io` |
| API backend | `https://api.34-136-168-229.nip.io` | `https://api.34-57-191-185.nip.io` |
| Keycloak | `https://auth.34-136-168-229.nip.io` | `https://auth.34-57-191-185.nip.io` |
| Grafana | `https://grafana.34-136-168-229.nip.io` | `https://grafana.34-57-191-185.nip.io` |
| Aprobación manual antes de desplegar | No | Sí (GitHub Environment `production`) |

## 2. Cómo se configuraron las VMs

Ambas VMs se configuraron de forma **idéntica**, con valores propios de cada ambiente (llaves, contraseñas, hostnames). Ninguna comparte credenciales con la otra.

### 2.1 Infraestructura base (GCP)

1. **Instancia Compute Engine**, tamaño similar (aprovisionada con los créditos gratuitos del trial de GCP).
2. **IP externa estática reservada** (`gcloud compute addresses create ... --addresses=<ip> --region=us-central1`) — sin esto, la IP es efímera y puede cambiar al reiniciar la VM, rompiendo el DNS de `nip.io` que depende de que la IP no cambie.
3. **Regla de firewall VPC** (`allow-sistema-gestion-web`, aplica a toda la red `default`, no a una VM específica) permitiendo entrada TCP a los puertos `22` (SSH), `5173` (frontend), `8081` (backend), `80` y `443` (Nginx/Let's Encrypt). El puerto `3000` de Grafana **nunca se abrió** — se accede solo a través de Nginx en el 443, igual que los demás servicios.

### 2.2 Usuario y acceso SSH

1. Usuario dedicado `deploy` (sin privilegios de `sudo`, solo pertenece al grupo `docker`) — nunca se usa el usuario personal de GCP para el despliegue automatizado.
2. Un par de llaves SSH `ed25519` **dedicado por ambiente** (`staging_deploy_key`, `production_deploy_key`), generado en la máquina local y autorizado únicamente en `/home/deploy/.ssh/authorized_keys` de su VM correspondiente. GitHub Actions usa la llave privada (guardada como secret) para conectarse por SSH y ejecutar el deploy.

### 2.3 Docker

Docker Engine + el plugin de Compose, instalados desde el repositorio oficial de Docker (no el paquete `docker.io` de Ubuntu, que suele ir desactualizado). El usuario `deploy` puede correr `docker`/`docker compose` sin `sudo` por pertenecer al grupo `docker`.

### 2.4 El repositorio en la VM

El repo se clona una sola vez en `/opt/sistema-gestion` (dueño: `deploy`). GitHub Actions **nunca hace `git clone`** en cada deploy — el script SSH hace:

```bash
git fetch origin <rama>
git checkout <rama>
git reset --hard origin/<rama>
```

Esto deja el working tree exactamente igual al remoto sin importar en qué rama haya quedado el clone anteriormente (relevante porque el clone inicial siempre queda en la rama por defecto del repo, no en `staging`/`main`).

### 2.5 El archivo `.env`

Un `.env` con las variables que pide `docker-compose.yml` (`POSTGRES_PASSWORD`, `KEYCLOAK_ADMIN_PASSWORD`, etc.), creado **una sola vez a mano** en cada VM, con valores generados con `openssl rand -hex 16` directamente en la propia VM — nunca pasan por GitHub Actions ni por el chat. Dos excepciones que sí se dejaron con valor fijo y conocido en ambos ambientes: `SEED_ADMIN_PASSWORD=admin123` y `SEED_EMPLOYEE_PASSWORD=employee123`, porque los tests E2E de Playwright y la demo en vivo dependen de poder loguearse con un usuario conocido.

A diferencia del job efímero de CI (que genera un `.env` nuevo con contraseñas aleatorias en cada corrida, y lo descarta al terminar), este `.env` es **persistente** — si cambiara en cada deploy, Postgres/Keycloak quedarían con credenciales desincronizadas entre reinicios del contenedor.

### 2.6 Nginx + TLS (Let's Encrypt vía nip.io)

Este fue el paso que casi queda afuera del alcance original, pero resultó ser **obligatorio, no opcional**: los navegadores modernos solo exponen la Web Crypto API (`crypto.subtle`) en contextos seguros (HTTPS o `localhost`), y `keycloak-js` la necesita para generar el PKCE code challenge del login. Sirviendo la app en HTTP plano sobre una IP pública, el login estaba roto en el 100% de los navegadores — no era una recomendación de seguridad, era un bloqueador funcional real (confirmado con `window.isSecureContext === false` y `crypto.subtle === undefined` en la consola del navegador).

La solución, sin comprar un dominio:

1. **[nip.io](https://nip.io)**: servicio de DNS gratuito que resuelve cualquier hostname que contenga una IP (`algo.34-136-168-229.nip.io`) directamente a esa IP. Se usó un subdominio por servicio: `staging`/`api`/`auth`/`grafana` (o `production` en vez de `staging`) por cada IP.
2. **Nginx** instalado directamente en la VM (fuera de Docker Compose), como reverse proxy: cada subdominio tiene su propio `server { server_name ...; proxy_pass http://127.0.0.1:<puerto>; }` apuntando al puerto que Docker Compose ya publica en `localhost` (`5173` frontend, `8081` backend, `8080` Keycloak, `3000` Grafana). Los archivos fuente están en `deploy/nginx/staging.conf` y `deploy/nginx/production.conf` del repo (documentados, pero no se auto-despliegan — es una configuración de infraestructura que se instala una sola vez a mano).
3. **Certbot** (`python3-certbot-nginx`), con `certbot --nginx -d <host1> -d <host2> -d <host3> -d <host4>`: detecta los `server_name` de los bloques HTTP existentes, obtiene un certificado SAN de Let's Encrypt que cubre los 4 hostnames en un solo certificado, y **reescribe automáticamente** la config de Nginx agregando `listen 443 ssl`, las rutas del certificado, y el redirect `80 → 443`. La renovación queda automática vía el systemd timer que instala el propio paquete de `certbot`.

### 2.7 Ajustes de la app para funcionar detrás del proxy HTTPS

Arreglar el transporte (Nginx+TLS) no alcanzaba — varias piezas de la app tenían hostnames de `localhost` hardcodeados que había que hacer configurables por ambiente:

- **Keycloak**: `KC_HOSTNAME` pasó de `localhost` a la URL pública completa (`https://auth.<ip>.nip.io`), más `KC_PROXY_HEADERS: xforwarded` para que confíe en el header `X-Forwarded-Proto` que le manda Nginx (si no, Keycloak no sabe que la conexión original del navegador fue HTTPS).
- **Backend (Spring Boot)**: `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` y `APP_CORS_ALLOWED_ORIGINS` pasaron a apuntar a los hostnames públicos. El `JWK_SET_URI` se dejó apuntando al hostname interno de Docker (`http://keycloak:8080/...`) porque esa llamada nunca sale del navegador — es tráfico contenedor-a-contenedor.
- **Frontend**: las URLs de Keycloak y del backend (`VITE_KEYCLOAK_URL`, `VITE_API_BASE_URL`) se volvieron variables de entorno leídas por Vite en build-time (antes estaban hardcodeadas a `localhost`). El Content-Security-Policy del `nginx.conf` del contenedor del frontend (que también tenía `connect-src` hardcodeado a `localhost`) se convirtió en un *template* (`nginx.conf.template`) que la propia imagen oficial de `nginx:alpine` rellena en **runtime** vía `envsubst`, usando variables `APP_ORIGIN`/`KEYCLOAK_ORIGIN` pasadas por `docker-compose.*.yml` — así no hace falta reconstruir la imagen si cambia el CSP.
- **Grafana**: `GF_SERVER_ROOT_URL` al hostname público — Grafana 11.x valida el header `Origin` contra su `root_url` para protección CSRF, y sin este ajuste el login fallaría por el mismo tipo de problema que Keycloak.

**Limitación aceptada conscientemente**: las URLs de Keycloak/backend del frontend quedan *horneadas* en el bundle de JS en tiempo de build (así es como funciona Vite) — por eso Staging y Production necesitan cada uno su propia imagen Docker del frontend, en vez de promover la misma imagen entre ambientes. Es una desviación del ideal de "un solo artefacto inmutable", aceptable para el alcance de este proyecto.

## 3. Cómo funciona el pipeline de GitHub Actions

Archivo: [`.github/workflows/ci.yml`](../.github/workflows/ci.yml). Se dispara en `push`/`pull_request` a `main`, `master`, `development` y `staging`, más `workflow_dispatch` manual y un cron nocturno (solo para el job de performance testing).

### 3.1 Jobs y su orden de dependencia

```
build-and-test ─┬─→ sonarcloud ─┬─→ docker-build-push-staging ──→ deploy-staging
                 │               └─→ docker-build-push-production ──→ deploy-production
                 └─→ performance-tests (solo workflow_dispatch/schedule)
```

1. **`build-and-test`**: compila, corre unit/integration/contract/data tests, verifica cobertura JaCoCo (mínimo 60%, bloqueante), OWASP Dependency-Check, `npm audit`, y levanta un `docker compose up` **efímero** (se destruye al final del job) para correr E2E con Playwright y los baseline scans de OWASP ZAP contra ese stack temporal.

2. **`sonarcloud`**: análisis de calidad + Quality Gate. Solo corre en Pull Requests o en push a `main` — **no** en `development`/`staging`, porque el plan gratuito de SonarCloud no soporta análisis de ramas intermedias (da error "Not authorized"). El bloqueo real del gate lo hace una sola propiedad de Gradle, `sonar.qualitygate.wait=true` (en `build.gradle`): hace que la propia tarea `sonar` haga polling contra la API de SonarCloud y termine con código de salida ≠ 0 si el gate falla — no requiere webhook ni configuración especial de Jenkins/GitHub, funciona igual en cualquier CI.

3. **`docker-build-push-staging`** (solo push a `staging`) / **`docker-build-push-production`** (solo push a `main`): construyen y publican las imágenes de backend y frontend a GitHub Container Registry (`ghcr.io/christiandgf/sistema-gestion-{backend,frontend}`), etiquetadas con el SHA del commit y con el nombre del ambiente (`staging`/`production`, más `latest` en production). El build del frontend recibe `VITE_KEYCLOAK_URL`/`VITE_API_BASE_URL` como *build-args* con los hostnames HTTPS de ese ambiente específico.

4. **`deploy-staging`** / **`deploy-production`**: se conectan por SSH a la VM correspondiente (`appleboy/ssh-action`) y ejecutan `git fetch/checkout/reset --hard` + `docker compose pull` + `docker compose up -d` usando el override de ese ambiente (`docker-compose.staging.yml` o `docker-compose.production.yml`, que reemplazan `build: .` por `image: ghcr.io/...`). Termina con un *smoke check* que reintenta contra `/actuator/health` cada 10 segundos hasta 2 minutos, en vez de un `sleep` fijo — Spring Boot con migraciones de Flyway + fetch de JWKS de Keycloak puede tardar más de lo que parece razonable a primera vista en un `docker compose up` en frío.

   **`deploy-production` requiere aprobación manual**: el GitHub Environment `production` tiene configurado un revisor obligatorio, así que aunque el push a `main` dispare el job, queda pausado esperando aprobación en la pestaña Actions antes de tocar la VM.

### 3.2 Secrets usados

| Secret | Para qué |
|---|---|
| `STAGING_SSH_HOST` / `STAGING_SSH_USER` / `STAGING_SSH_PRIVATE_KEY` | Conexión SSH al deploy de Staging |
| `PROD_SSH_HOST` / `PROD_SSH_USER` / `PROD_SSH_PRIVATE_KEY` | Conexión SSH al deploy de Production |
| `SONAR_TOKEN` | Autenticación contra SonarCloud |
| `NVD_API_KEY` | Evita rate-limiting de OWASP Dependency-Check contra la NVD |
| `GITHUB_TOKEN` | Automático (provisto por GitHub Actions) — login a GHCR |

Las IPs de las VMs y los hostnames de `nip.io` están **hardcodeados directamente en el YAML**, no como secrets — son datos públicos (la IP externa es visible para cualquiera que le haga `ping` al dominio), y además GitHub Actions no permite usar el contexto `secrets` dentro de `environment.url` (una restricción real que rompió el primer intento del pipeline).

### 3.3 Gotchas de GitHub Actions que costó tiempo encontrar

Vale la pena documentarlos porque ninguno es intuitivo y no salen en la documentación básica:

1. **`environment.url` no acepta el contexto `secrets`** — solo `github`, `inputs`, `vars`, `needs`, `strategy`, `matrix`, `env`. Hubo que hardcodear las IPs ahí.
2. **El operador `in (...)` no existe** en las expresiones de GitHub Actions (a diferencia de Python/SQL) — solo `==`, `!=`, `&&`, `||` y funciones.
3. **El "skip-cascade" por defecto no se desactiva comparando `needs.X.result` como string.** Si un job en `needs:` queda `skipped`, todo lo que dependa de él (directa **o transitivamente**) también se salta por defecto, sin importar el `if:` — a menos que ese `if:` use explícitamente `!failure()`, `!cancelled()` o `always()`. Esto afectó tanto al job de build/push (dependencia directa de `sonarcloud`) como al de deploy (dependencia *indirecta*, dos niveles más abajo) — cada uno rompió por separado.
4. **GHCR exige nombres de repositorio en minúsculas** — `${{ github.repository_owner }}` resuelve al usuario de GitHub tal cual (con mayúsculas), y no hay función `lower()` en las expresiones. Se hardcodeó el literal en minúsculas.

## 4. Cómo se prueba que todo esto funciona de verdad

No basta con que el pipeline termine en verde — se verificó manualmente contra la infraestructura real:

- Login completo por navegador (Playwright/browser tool) contra `https://staging.34-136-168-229.nip.io` y `https://production.34-57-191-185.nip.io`, confirmando cero errores de consola.
- `curl` contra los 4 subdominios de cada ambiente confirmando certificado TLS válido (no autofirmado) y el código de estado esperado.
- Conexión SSH directa a cada VM para confirmar `/actuator/health` con `status: UP` y los 9 contenedores (`app`, `frontend`, `db`, `keycloak`, y el stack de observabilidad) corriendo.
