# Performance Testing (k6)

Pruebas de carga, estrés y pico para el Sistema de Gestión de Inventarios, usando
[k6](https://k6.io). Cubre el requisito obligatorio de la consigna: *Stress testing,
Load testing, Concurrent users, Tiempo de respuesta y Throughput* (ver
`performance_testing_plan.md` en la raíz del repo para el plan completo).

## Requisitos

- [k6](https://k6.io/docs/get-started/installation/) instalado localmente, **o** Docker
  (imagen `grafana/k6`).
- El sistema corriendo y accesible: como mínimo `db`, `keycloak` y `app`
  (`docker-compose up -d db keycloak app`).

## Estructura

```
performance-tests/
├── config.js          # BASE_URL, config de Keycloak, thresholds compartidos
├── lib/
│   ├── auth.js         # login (password grant) y refresh de JWT
│   ├── session.js       # token por VU, con refresh automático cada N iteraciones
│   ├── seed.js          # crea/limpia productos de prueba vía la API real
│   ├── mix.js            # mezcla de tráfico (qué endpoint golpear y con qué peso)
│   └── report.js          # genera reportes HTML/JSON con k6-reporter
├── scenarios/
│   ├── smoke.js   # 1 VU, 1 iteración — valida que el ambiente esté listo
│   ├── load.js    # 20 VUs sostenidos — concurrencia esperada
│   ├── stress.js  # rampa hasta 300 VUs — busca el punto de quiebre
│   └── spike.js   # salto abrupto de VUs — valida recuperación
└── reports/       # salida de cada corrida (HTML/JSON), gitignored
```

## Cómo correr un escenario

```bash
# Contra el ambiente local (valores por defecto de config.js)
k6 run performance-tests/scenarios/smoke.js
k6 run performance-tests/scenarios/load.js
k6 run performance-tests/scenarios/stress.js
k6 run performance-tests/scenarios/spike.js

# Contra otro ambiente (ej. staging)
k6 run -e BASE_URL=https://staging.example.com \
       -e KEYCLOAK_URL=https://staging-auth.example.com \
       performance-tests/scenarios/load.js
```

Con Docker (sin instalar k6 localmente), corriendo en la red del host:

```bash
docker run --rm --network host \
  -v "$(pwd)/performance-tests:/scripts:ro" \
  -v "$(pwd)/performance-tests/reports:/scripts/reports:rw" \
  grafana/k6:latest run /scripts/scenarios/load.js
```

**Orden recomendado**: siempre correr `smoke.js` primero. Si falla, no tiene sentido
correr `load.js`/`stress.js` — el problema está en la configuración del ambiente, no en
su capacidad.

## Variables de entorno soportadas

| Variable | Default | Descripción |
|---|---|---|
| `BASE_URL` | `http://localhost:8081` | URL del backend |
| `KEYCLOAK_URL` | `http://localhost:8080` | URL de Keycloak |
| `KEYCLOAK_REALM` | `sistema-gestion` | Realm a usar |
| `KEYCLOAK_CLIENT_ID` | `sistema-gestion-client` | Cliente público (password grant) |
| `PERF_TEST_USERNAME` | `employee` | Usuario semilla del realm |
| `PERF_TEST_PASSWORD` | `employee123` | Password del usuario semilla |

## Leer los resultados

Cada corrida deja en `performance-tests/reports/`:
- `<scenario>-summary.html`: reporte visual (abrir en el navegador).
- `<scenario>-summary.json`: métricas completas (`http_req_duration`, `http_reqs`,
  `http_req_failed`, checks) para análisis o comparación entre corridas.

Los thresholds definidos en `config.js` (`BASELINE_THRESHOLDS`) hacen que k6 termine con
código de salida distinto de cero si se incumplen — esto es lo que falla el job/stage de
CI en `load.js`. `stress.js` y `spike.js` usan thresholds más laxos a propósito: su
objetivo es *reportar* en qué punto degrada el sistema, no bloquear el build.

## CI/CD

- **GitHub Actions** (`.github/workflows/ci.yml`): job `performance-tests`, solo se
  dispara manualmente (`workflow_dispatch`) o de forma nocturna (`schedule`), para no
  bloquear cada PR.
- **Jenkins** (`Jenkinsfile`): stage `Performance Testing (k6)`, corre smoke + load en
  cada pipeline; `stress.js` es opcional vía el parámetro/env `RUN_STRESS_TEST=true`.

Ambos corren contra el stack ya desplegado por `docker-compose up`, no contra mocks.
