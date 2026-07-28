# Sistema de Gestión de Inventarios Empresarial

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ChristianDGF_Sistema-Gestion-FST-O-DSO&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ChristianDGF_Sistema-Gestion-FST-O-DSO)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=ChristianDGF_Sistema-Gestion-FST-O-DSO&metric=coverage)](https://sonarcloud.io/summary/new_code?id=ChristianDGF_Sistema-Gestion-FST-O-DSO)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=ChristianDGF_Sistema-Gestion-FST-O-DSO&metric=bugs)](https://sonarcloud.io/summary/new_code?id=ChristianDGF_Sistema-Gestion-FST-O-DSO)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=ChristianDGF_Sistema-Gestion-FST-O-DSO&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=ChristianDGF_Sistema-Gestion-FST-O-DSO)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=ChristianDGF_Sistema-Gestion-FST-O-DSO&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=ChristianDGF_Sistema-Gestion-FST-O-DSO)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=ChristianDGF_Sistema-Gestion-FST-O-DSO&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=ChristianDGF_Sistema-Gestion-FST-O-DSO)

Sistema moderno de gestión de inventarios orientado a pequeñas empresas, desarrollado como
Proyecto Final de la asignatura **Aseguramiento de Calidad de Software** (PUCMM). Implementa un
ecosistema completo de calidad: testing en múltiples niveles, seguridad aplicada (Keycloak/OAuth2/JWT),
observabilidad distribuida (Prometheus, Tempo, Loki, Grafana) y pipelines de CI/CD profesionales
(GitHub Actions + Jenkins) con despliegue automático a ambientes de Staging y Production.

## Tabla de contenidos

- [Stack tecnológico](#stack-tecnológico)
- [Arquitectura](#arquitectura)
- [Funcionalidades](#funcionalidades)
- [Seguridad y modelo de permisos](#seguridad-y-modelo-de-permisos)
- [Observabilidad](#observabilidad)
- [Testing](#testing)
- [CI/CD](#cicd)
- [Ambientes](#ambientes)
- [Cómo levantar el proyecto localmente](#cómo-levantar-el-proyecto-localmente)
- [Comandos útiles](#comandos-útiles)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Documentación adicional](#documentación-adicional)
- [Estrategia de ramas y contribución](#estrategia-de-ramas-y-contribución)

## Stack tecnológico

| Capa | Tecnologías |
|---|---|
| Backend | Java 21, Spring Boot 3, Spring Security (OAuth2 Resource Server), Hibernate, Hibernate Envers (auditoría), Flyway (migraciones) |
| Frontend | React 19, Vite, Tailwind CSS, keycloak-js |
| Base de datos | PostgreSQL 16 |
| Identidad y acceso | Keycloak 26 (OAuth2, JWT, roles, scopes) |
| Observabilidad | Prometheus, Grafana, Loki, Tempo, Grafana Alloy, Alertmanager, OpenTelemetry |
| Testing | JUnit 5, Mockito, Testcontainers, JaCoCo, Playwright (E2E), k6 (performance), OWASP ZAP (DAST) |
| Calidad de código | SonarCloud (Quality Gate bloqueante) |
| CI/CD | GitHub Actions, Jenkins (pipeline declarativo) |
| Infraestructura | Docker, Docker Compose, Nginx (reverse proxy + TLS en Staging/Production) |

## Arquitectura

### Flujo de ramas y ambientes

```
feature/*  →  development  →  staging  →  main
                (integración)  (VM Staging)   (VM Production)
```

Cada rama de promoción despliega a su propia VM en GCP corriendo el stack completo
(`docker-compose.yml`: backend, frontend, Postgres, Keycloak y observabilidad) detrás de un
reverse proxy Nginx con TLS. Detalle completo en [`docs/deployment.md`](docs/deployment.md).

### Observabilidad distribuida

```
                    +-----------+
                    |  Grafana  |
                    +-----+-----+
                          |
      +-------------------+-------------------+
      |                   |                   |
+-----+------+     +------+-----+      +------+-----+
| Prometheus |     |   Tempo    |      |    Loki    |
+-----+------+     +------+-----+      +------+-----+
      |                   ^                   ^
      |                   |                   |
      +------------+ Alloy +------------------+
                   |4317/4318|
                   +----+----+
                        |
                        v
                  +-----+------+
                  |Alertmanager|
                  +------------+
```

La aplicación exporta trazas hacia Alloy vía OTLP HTTP (`/v1/traces`); Alloy reenvía a Tempo por
gRPC (puerto `4317`). Ver la nota de troubleshooting en [`CLAUDE.md`](CLAUDE.md) para el porqué de
este diseño.

### API stateless

El backend es completamente stateless: la sesión se maneja mediante JWT emitidos por Keycloak.
Cada endpoint crítico se protege con `@PreAuthorize` a nivel de método, validando el scope exacto
requerido (no el nombre del rol). Los cambios sobre entidades auditadas quedan trazados en tablas
`_aud` vía Hibernate Envers.

## Funcionalidades

**Gestión de productos**: alta, edición y baja de productos (SKU, descripción, categoría, precio,
cantidad, stock mínimo, estado); listado paginado con búsqueda, filtros y ordenamiento.

**Control de stock**: entradas/salidas de inventario, alertas automáticas al llegar al stock
mínimo, historial completo de movimientos (fecha, usuario, tipo, cantidad anterior/nueva,
observaciones) y auditoría de cambios con Hibernate Envers.

**API empresarial**: API REST documentada con OpenAPI/Swagger UI, exponiendo CRUD de productos,
consulta de inventario, movimientos de stock y reportes.

**Dashboard**: tablero con productos críticos, productos más vendidos, historial reciente,
métricas del sistema e indicadores operacionales.

## Seguridad y modelo de permisos

Autenticación y autorización 100% delegadas a Keycloak vía OAuth2/JWT, con un modelo granular
basado en scopes (no roles simples tipo "Admin"/"Empleado"): cada operación crítica valida el
permiso exacto que necesita.

| Módulo | Permiso | Descripción |
|---|---|---|
| Productos | `product:view` | Ver productos |
| Productos | `product:manage` | Crear, editar y eliminar productos |
| Stock | `stock:view` | Ver existencias e historial |
| Stock | `stock:manage` | Registrar entradas, salidas y ajustes |
| Reportes | `report:view` | Ver reportes y dashboard |
| Seguridad | `user:manage` | Gestionar usuarios, roles y permisos |
| Auditoría | `audit:view` | Consultar auditoría del sistema |

Los roles de Keycloak se construyen combinando estos scopes. Cobertura de seguridad, defectos
encontrados/corregidos (JWT, CORS, permisos, dependencias vulnerables, OWASP ZAP) y el flujo de
gestión de secretos están documentados en [`docs/security-testing.md`](docs/security-testing.md).

## Observabilidad

| Pilar | Herramienta | Qué cubre |
|---|---|---|
| Métricas | Prometheus + Micrometer | CPU, memoria, JVM, latencia, throughput, error rate, pool de conexiones |
| Logs | Loki (via `loki-logback-appender`) | Logs estructurados con `traceId`/`spanId`/`correlationId`/usuario/endpoint en el MDC |
| Trazas | Tempo + OpenTelemetry | Request tracing, llamadas externas, errores distribuidos |
| Dashboards | Grafana | Paneles de infraestructura, aplicación, negocio y seguridad |
| Alertas | Alertmanager | CPU alto, error rate elevado, latencia alta, servicios caídos, fallos de autenticación |

Accesos locales: Grafana en `http://localhost:3000` (`admin`/`admin` por defecto vía `.env`).

## Testing

El proyecto cubre todos los niveles de testing exigidos por la consigna:

| Nivel | Herramientas | Dónde vive |
|---|---|---|
| Unit | JUnit 5, Mockito | `src/test/java/.../service`, `.../controller` |
| Integration | Testcontainers (Postgres, Keycloak) | `src/test/java/.../integration` |
| Contract / API | RestAssured + validación de OpenAPI | `src/test/java/.../contract` |
| Data | Testcontainers (Postgres real, sin mocks) | `src/test/java/.../data` — ver [`docs/data-testing.md`](docs/data-testing.md) |
| Security | JWT, CORS, matriz de permisos, OWASP ZAP, Dependency-Check | `src/test/java/.../security` — ver [`docs/security-testing.md`](docs/security-testing.md) |
| E2E | Playwright (login, CRUD, RBAC, responsive, visual) | `frontend/tests/` |
| Performance | k6 (smoke, load, stress, spike) | `performance-tests/` |

```bash
./gradlew test                              # unit + integration + contract + data + security
./gradlew jacocoTestCoverageVerification     # cobertura mínima 60%, bloqueante
npx playwright test                          # E2E (dentro de /frontend)
```

## CI/CD

Dos pipelines equivalentes, ambos ejecutando build, tests, security scans, coverage gate y build
de imágenes Docker:

- **GitHub Actions** ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)): dispara en push/PR a
  `main`/`master`/`development`/`staging`. Encadena `build-and-test → sonarcloud → docker build →
  deploy`, con despliegue automático a Staging (rama `staging`) y a Production (rama `main`, con
  aprobación manual vía GitHub Environment).
- **Jenkins** ([`Jenkinsfile`](Jenkinsfile)): pipeline declarativo equivalente (Checkout, Build,
  Unit/Contract/Integration/Data tests, SonarCloud, lint de frontend, Security Scan (SCA), Docker
  Build & Push).

Las pruebas de integración, API, E2E y seguridad corren contra el sistema realmente desplegado
(no solo durante el build de la imagen). Detalle completo del pipeline, secrets utilizados y los
"gotchas" encontrados en [`docs/deployment.md`](docs/deployment.md).

## Ambientes

| Ambiente | Rama | Frontend | Backend | Notas |
|---|---|---|---|---|
| Development | `development`/`feature/*` | `http://localhost:5173` | `http://localhost:8081` | Local, vía `docker-compose up` |
| Staging | `staging` | `https://staging.34-136-168-229.nip.io` | `https://api.34-136-168-229.nip.io` | VM GCP, despliegue automático |
| Production | `main` | `https://production.34-57-191-185.nip.io` | `https://api.34-57-191-185.nip.io` | VM GCP, requiere aprobación manual |

## Cómo levantar el proyecto localmente

```bash
cp .env.example .env        # completar los valores requeridos (ver comentarios del archivo)
docker-compose up -d        # backend, frontend, Postgres, Keycloak y stack de observabilidad
docker-compose ps           # verificar estado de los servicios
```

| Servicio | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| Keycloak Admin | http://localhost:8080 (`admin`/`admin`) |
| Grafana | http://localhost:3000 (`admin`/`admin`) |

## Comandos útiles

### Backend (Gradle)

```bash
./gradlew build                              # compilar y correr pruebas
./gradlew test                               # unit + integración (levanta Testcontainers)
./gradlew jacocoTestCoverageVerification      # validar cobertura mínima (60%)
./gradlew jacocoTestReport                    # reporte HTML en build/reports/jacoco/test/html/index.html
```

### Frontend (Node)

```bash
npm install            # dentro de /frontend
npm run dev             # entorno de desarrollo
npx playwright test     # pruebas E2E
```

## Estructura del proyecto

```text
Sistema-Gestion-FST-O-DSO/
├── .github/                  # Workflows de GitHub Actions (CI/CD)
├── docs/                     # Guías técnicas (testing, seguridad, despliegue)
├── frontend/                 # Aplicación SPA en React
│   ├── src/
│   │   ├── api/               # Configuración de Axios e interceptores
│   │   ├── auth/               # Contexto y configuración de Keycloak-js
│   │   ├── components/        # Componentes visuales reutilizables
│   │   ├── layouts/            # Estructuras de página principal
│   │   └── main.jsx            # Punto de entrada de React
│   ├── tests/                 # Pruebas E2E (Playwright)
│   └── Dockerfile             # Multi-stage build para Vite + Nginx
├── observability/            # Configuración del stack de monitoreo
│   ├── alloy/                  # Grafana Alloy (Colector)
│   ├── grafana/                # Dashboards y datasources
│   ├── loki/                   # Gestor de Logs
│   ├── prometheus/             # Motor de métricas
│   └── tempo/                  # Trazas distribuidas
├── performance-tests/         # Escenarios k6 (smoke, load, stress, spike)
├── src/                       # Backend Spring Boot (código fuente Java)
│   ├── main/
│   │   ├── java/proyecto/sistemaGestion/
│   │   │   ├── config/           # Configuración global (SecurityConfig)
│   │   │   ├── controller/       # Endpoints REST expuestos (/api/v1/...)
│   │   │   ├── dto/               # Objetos de Transferencia de Datos
│   │   │   ├── entity/            # Entidades JPA (@Entity, @Audited)
│   │   │   ├── enums/              # Enumeraciones del sistema
│   │   │   ├── exception/         # Manejo global de excepciones
│   │   │   ├── repository/        # Interfaces Spring Data JPA
│   │   │   └── service/            # Lógica de negocio y transacciones
│   │   └── resources/
│   │       ├── db/migration/      # Scripts de Flyway (V1__, V2__...)
│   │       └── application.properties
│   └── test/                  # Pruebas automatizadas (unit, integration, contract, data, security)
├── Dockerfile                 # Multi-stage build para el backend Spring Boot
├── docker-compose.yml         # Orquestación de servicios locales
├── build.gradle                # Configuración de Gradle y dependencias
└── Jenkinsfile                 # Pipeline de integración continua para Jenkins
```

## Documentación adicional

- [`docs/data-testing.md`](docs/data-testing.md) — migraciones, constraints, integridad de datos,
  duplicados y seeds.
- [`docs/security-testing.md`](docs/security-testing.md) — JWT, permisos, CORS, OWASP ZAP,
  dependency scanning y gestión de secretos.
- [`docs/deployment.md`](docs/deployment.md) — arquitectura de Staging/Production, configuración
  de las VMs de GCP y funcionamiento detallado del pipeline de GitHub Actions.
- [`performance-tests/README.md`](performance-tests/README.md) — escenarios de carga y cómo
  correrlos.
- [`CLAUDE.md`](CLAUDE.md) — notas arquitectónicas y troubleshooting (IPv6/Docker, OTLP/Tempo,
  panel de trazas de Grafana).

## Estrategia de ramas y contribución

```
feature/*  →  development  →  staging  →  main
```

Cambios funcionales entran por `feature/*`/`fix/*` y se integran a `development` vía Pull Request
con Conventional Commits. `staging` y `main` se actualizan por promoción y disparan despliegue
automático a sus respectivas VMs. Cambios de documentación pura (como este) pueden integrarse
directamente a `development` sin disparar el pipeline (`[skip ci]`), al no afectar build, tests ni
despliegue.
