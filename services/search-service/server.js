'use strict';
const crypto = require('crypto');
const express = require('express');
const pino = require('pino');
const pinoHttp = require('pino-http');

const SERVICE_NAME = process.env.SERVICE_NAME || 'search-service';
const PORT = Number(process.env.PORT || 3000);

// All logs are structured JSON on stdout (12-factor), ready for
// Fluent Bit / Loki / ELK collection from the container runtime.
const logger = pino({
  level: process.env.LOG_LEVEL || 'info',
  timestamp: pino.stdTimeFunctions.isoTime,
  base: { service: SERVICE_NAME, version: process.env.SERVICE_VERSION || '1.0.0' },
  formatters: { level: (label) => ({ level: label }) }
});

const app = express();
app.use(express.json());
// --- Trace ID propagation -------------------------------------------------
// Accept X-Trace-Id from the caller (falling back to X-Request-Id), otherwise
// mint one. The id is echoed on the response and stamped on every log line so
// a single request can be followed across the gateway and every service.
app.use((req, res, next) => {
  const incoming = String(req.headers['x-trace-id'] || req.headers['x-request-id'] || '')
    .trim().replace(/[^\w.:-]/g, '').slice(0, 128);
  req.traceId = incoming || `trace-${crypto.randomUUID()}`;
  res.setHeader('X-Trace-Id', req.traceId);
  next();
});
// Probe/status endpoints are polled every few seconds by Kubernetes and the
// gateway health aggregator and would drown out real traffic in the logs.
const LOG_IGNORED_PATHS = new Set(['/health', '/ready']);

app.use(pinoHttp({
  logger,
  // Two flat, grep-able lines per request — 'request received' with the full
  // request detail, and 'request completed/failed' with status + duration —
  // every line carrying traceId / requestUri / client fields at the top level.
  autoLogging: { ignore: (req) => LOG_IGNORED_PATHS.has((req.url || '').split('?')[0]) },
  customAttributeKeys: { responseTime: 'durationMs' },
  customLogLevel: (req, res, err) =>
    (err || res.statusCode >= 500) ? 'error' : res.statusCode >= 400 ? 'warn' : 'info',
  customReceivedMessage: (req) => `request received: ${req.method} ${req.originalUrl || req.url}`,
  customSuccessMessage: (req, res) => `request completed: ${req.method} ${req.originalUrl || req.url} -> ${res.statusCode}`,
  customErrorMessage: (req, res) => `request failed: ${req.method} ${req.originalUrl || req.url} -> ${res.statusCode}`,
  // Drop the bulky nested req/res dumps; the useful fields are emitted flat
  // via customProps so lines match the platform-wide log shape.
  serializers: { req: () => undefined, res: (res) => ({ statusCode: res.statusCode }) },
  customProps: (req) => {
    // pino-http applies customProps to the request child logger AND to the
    // completion log; the guard binds the fields exactly once per request.
    if (req._logPropsBound) return {};
    req._logPropsBound = true;
    return {
      traceId: req.traceId,
      requestId: req.headers['x-request-id'] || undefined,
      requestUri: req.originalUrl || req.url,
      method: req.method,
      query: Object.keys(req.query || {}).length ? req.query : undefined,
      contentLength: req.headers['content-length'] ? Number(req.headers['content-length']) : undefined,
      clientIp: String(req.headers['x-forwarded-for'] || '').split(',')[0].trim() || req.socket.remoteAddress,
      userAgent: req.headers['user-agent'] ? String(req.headers['user-agent']).slice(0, 256) : undefined
    };
  }
}));

// --- Kubernetes probes -------------------------------------------------
app.get('/health', (req, res) => res.json({ status: 'ok', service: SERVICE_NAME }));
app.get('/ready', (req, res) => res.json({ ready: true, service: SERVICE_NAME }));

// --- Full-text search across the catalog ---
// Mirrors product-catalog-service's own seed data (see that service's
// server.js) so a hit here is a real, fully-shaped Product the client can
// render/add-to-cart exactly like any /products result — plus a `terms`
// field of extra searchable synonyms that isn't returned to the client.
const index = [
  { id: 'p-1', name: 'Levain Country Loaf', category: 'bread', price: 8.50, description: '48-hour fermented sourdough, dark bake.', terms: 'sourdough bread levain country loaf' },
  { id: 'p-2', name: 'Seeded Rye', category: 'bread', price: 7.00, description: 'Dense Danish-style rye with sunflower and flax.', terms: 'rye bread seeded danish' },
  { id: 'p-3', name: 'Butter Croissant', category: 'viennoiserie', price: 4.25, description: '27 layers of cultured butter.', terms: 'croissant butter pastry viennoiserie' },
  { id: 'p-4', name: 'Cardamom Knot', category: 'viennoiserie', price: 4.75, description: 'Swedish-style bun, freshly ground cardamom.', terms: 'cardamom bun knot swedish' },
  { id: 'p-5', name: 'Pain au Chocolat', category: 'viennoiserie', price: 4.50, description: 'Two batons of 70% chocolate.', terms: 'chocolate pain au chocolat pastry' },
  { id: 'p-6', name: 'Morning Bun', category: 'viennoiserie', price: 4.50, description: 'Croissant dough, orange zest, muscovado.', terms: 'morning bun orange croissant' },
  { id: 'p-7', name: 'Pistachio Financier', category: 'patisserie', price: 3.75, description: 'Brown-butter almond cake, Sicilian pistachio.', terms: 'pistachio financier cake almond' },
  { id: 'p-8', name: 'Sour Cherry Galette', category: 'patisserie', price: 6.25, description: 'Rye crust, whole sour cherries.', terms: 'cherry galette pie tart' },
  { id: 'p-9', name: 'Canele', category: 'patisserie', price: 3.50, description: 'Rum and vanilla, caramelised copper-mould crust.', terms: 'canele rum vanilla' },
  { id: 'p-10', name: 'Baguette Tradition', category: 'bread', price: 3.90, description: 'Slow-fermented, thin crackling crust.', terms: 'baguette french bread tradition' },
  { id: 'p-11', name: 'Focaccia al Rosmarino', category: 'bread', price: 5.50, description: 'Olive oil crumb, flaky salt, rosemary.', terms: 'focaccia rosemary olive bread' },
  { id: 'p-12', name: 'Espresso Walnut Babka', category: 'patisserie', price: 9.00, description: 'Twisted brioche, espresso frangipane.', terms: 'babka espresso walnut brioche' }
];

// Returns a bare JSON array of Products — same shape as GET /products —
// so the Android client's `Response<List<Product>>` can parse it directly.
// This used to return { query, hits } with hit objects missing
// category/price/description, which the client can't deserialize as
// Product at all; every search request failed client-side as a result.
app.get('/search', (req, res) => {
  const q = String(req.query.q || '').toLowerCase().trim();
  if (!q) return res.status(400).json({ error: 'query parameter q is required' });
  const hits = index
    .filter(d => d.terms.includes(q) || d.name.toLowerCase().includes(q))
    .map(({ id, name, category, price, description }) => ({ id, name, category, price, description }));
  req.log.info({ event: 'search_executed', query: q, hits: hits.length }, 'search served');
  res.json(hits);
});

// --- 404 + error handling ----------------------------------------------
app.use((req, res) => res.status(404).json({ error: 'Route not found' }));
app.use((err, req, res, next) => {
  req.log.error({ event: 'unhandled_error', message: err.message }, 'request failed');
  res.status(500).json({ error: 'Internal server error', traceId: req.traceId });
});

const server = app.listen(PORT, () => logger.info({ event: 'service_started', port: PORT }, `${SERVICE_NAME} listening`));

for (const signal of ['SIGTERM', 'SIGINT']) {
  process.on(signal, () => {
    logger.info({ event: 'shutdown', signal }, 'shutting down gracefully');
    server.close(() => process.exit(0));
  });
}
