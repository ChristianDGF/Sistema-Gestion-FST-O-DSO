/**
 * tracing.js — Inicialización del SDK de OpenTelemetry Web
 *
 * Este módulo debe importarse como el PRIMER import en main.jsx para
 * garantizar que la instrumentación esté activa antes de que React monte
 * cualquier componente o se realice cualquier petición HTTP.
 *
 * Flujo de una traza E2E:
 *   Browser (span) → Alloy :4318 → Tempo
 *                              ↓ (traceparent header)
 *                          Backend Spring Boot (span hijo)
 */

import { WebTracerProvider } from '@opentelemetry/sdk-trace-web';
import { BatchSpanProcessor } from '@opentelemetry/sdk-trace-web';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
import { ZoneContextManager } from '@opentelemetry/context-zone';
import { Resource } from '@opentelemetry/resources';
import { ATTR_SERVICE_NAME, ATTR_SERVICE_VERSION } from '@opentelemetry/semantic-conventions';
import { registerInstrumentations } from '@opentelemetry/instrumentation';
import { XMLHttpRequestInstrumentation } from '@opentelemetry/instrumentation-xml-http-request';
import { DocumentLoadInstrumentation } from '@opentelemetry/instrumentation-document-load';

// ─── Configuración ──────────────────────────────────────────────────────────

const COLLECTOR_URL = import.meta.env.VITE_OTEL_EXPORTER_URL ?? 'http://localhost:4318/v1/traces';
const SERVICE_NAME  = import.meta.env.VITE_SERVICE_NAME      ?? 'sistemaGestion-frontend';
const SERVICE_VERSION = '1.0.0';

// URL base del backend para propagar el contexto de traza (traceparent header)
const BACKEND_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081';

// ─── Resource (identifica el servicio en Grafana) ───────────────────────────

const resource = new Resource({
  [ATTR_SERVICE_NAME]:    SERVICE_NAME,
  [ATTR_SERVICE_VERSION]: SERVICE_VERSION,
});

// ─── Exportador OTLP/HTTP → Alloy → Tempo ───────────────────────────────────

const exporter = new OTLPTraceExporter({
  url: COLLECTOR_URL,
  // No incluir credenciales (CORS preflight fallaría con cookies/auth headers)
  headers: {},
});

// ─── Provider y Processor ───────────────────────────────────────────────────

const provider = new WebTracerProvider({
  resource,
  // BatchSpanProcessor agrupa los spans y los envía en lotes para minimizar
  // el overhead de red (vs SimpleSpanProcessor que envía span a span)
  spanProcessors: [new BatchSpanProcessor(exporter)],
});

// ZoneContextManager mantiene el contexto de traza a través de callbacks
// asíncronos, Promises y event handlers del browser
provider.register({
  contextManager: new ZoneContextManager(),
});

// ─── Instrumentaciones automáticas ──────────────────────────────────────────

registerInstrumentations({
  instrumentations: [
    // Traza la carga inicial del documento HTML (DOMContentLoaded, load, etc.)
    new DocumentLoadInstrumentation(),

    // Traza todas las peticiones XHR (Axios usa XMLHttpRequest internamente).
    // propagateTraceHeaderCorsUrls inyecta el header W3C `traceparent` en las
    // peticiones al backend, conectando el span del browser con el span de Spring.
    new XMLHttpRequestInstrumentation({
      propagateTraceHeaderCorsUrls: [
        new RegExp(`^${BACKEND_BASE_URL.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}.*`),
      ],
      // Excluir peticiones al propio colector para evitar bucles
      ignoreUrls: [/localhost:4318/, /localhost:3100/],
    }),
  ],
});

export default provider;
