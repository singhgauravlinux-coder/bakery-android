'use strict';
const crypto = require('crypto');
const express = require('express');
const pino = require('pino');
const pinoHttp = require('pino-http');
const { Pool } = require('pg');
const { clientInfo } = require('./lib/client-info');
const { createAuditLogger } = require('./lib/audit');

const SERVICE_NAME = process.env.SERVICE_NAME || 'consent-service';
const PORT = Number(process.env.PORT || 3000);
const DATABASE_URL = process.env.DATABASE_URL || '';

// The set of things a customer can be asked to consent to. Kept as a
// closed list (rather than accepting any string) so a typo in a client
// ("locaton") silently creates a new, never-checked consent type instead
// of failing loudly. Add a new value here when the product adds a new
// consent prompt (e.g. "notifications", "analytics").
// for location extract.
const CONSENT_TYPES = new Set(['location']);

const logger = pino({
  level: process.env.LOG_LEVEL || 'info',
  timestamp: pino.stdTimeFunctions.isoTime,
  base: { service: SERVICE_NAME, version: process.env.SERVICE_VERSION || '1.0.0' },
  formatters: { level: (label) => ({ level: label }) }
});

const pool = DATABASE_URL ? new Pool({ connectionString: DATABASE_URL, max: 5 }) : null;
if (pool) pool.on('error', (err) => logger.error({ event: 'pg_pool_error', message: err.message }, 'postgres pool error'));

// Self-migrating (idempotent) — mirrored in db/migrations/0011_location_consents.sql.
// One row per (user, consent type); re-consenting overwrites in place via
// the upsert below, so "current status" is always a single row lookup —
// a full history of changes lives in security_audit_logs (via `audit`
// below) for anyone who needs the trail, not in this table.
const MIGRATION = `
  CREATE TABLE IF NOT EXISTS user_consents (
    user_id       TEXT NOT NULL,
    consent_type  TEXT NOT NULL,
    granted       BOOLEAN NOT NULL,
    source        TEXT NOT NULL DEFAULT 'app',
    responded_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, consent_type)
  );
  CREATE INDEX IF NOT EXISTS idx_user_consents_type ON user_consents (consent_type);
`;

const ROW = `user_id AS "userId", consent_type AS "consentType", granted, source,
  responded_at AS "respondedAt", updated_at AS "updatedAt"`;

const memory = new Map(); // key: `${userId}:${consentType}`

const store = pool ? {
  mode: 'postgres',
  async init() { await pool.query(MIGRATION); },
  async upsert(userId, consentType, granted, source) {
    const { rows } = await pool.query(
      `INSERT INTO user_consents (user_id, consent_type, granted, source)
       VALUES ($1, $2, $3, $4)
       ON CONFLICT (user_id, consent_type)
       DO UPDATE SET granted = $3, source = $4, updated_at = now()
       RETURNING ${ROW}`,
      [userId, consentType, granted, source]);
    return rows[0];
  },
  async get(userId, consentType) {
    const { rows } = await pool.query(
      `SELECT ${ROW} FROM user_consents WHERE user_id = $1 AND consent_type = $2`,
      [userId, consentType]);
    return rows[0] || null;
  },
  async listForUser(userId) {
    const { rows } = await pool.query(
      `SELECT ${ROW} FROM user_consents WHERE user_id = $1 ORDER BY consent_type`, [userId]);
    return rows;
  },
  async ping() { await pool.query('SELECT 1'); }
} : {
  mode: 'memory',
  async init() {},
  async upsert(userId, consentType, granted, source) {
    const record = {
      userId, consentType, granted, source,
      respondedAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
    memory.set(`${userId}:${consentType}`, record);
    return record;
  },
  async get(userId, consentType) { return memory.get(`${userId}:${consentType}`) || null; },
  async listForUser(userId) {
    return [...memory.values()].filter((r) => r.userId === userId);
  },
  async ping() {}
};

const audit = createAuditLogger({ pool, logger, service: SERVICE_NAME });

const app = express();
app.set('trust proxy', true);
app.use(express.json());
// --- Trace ID propagation -------------------------------------------------
app.use((req, res, next) => {
  const incoming = String(req.headers['x-trace-id'] || req.headers['x-request-id'] || '')
    .trim().replace(/[^\w.:-]/g, '').slice(0, 128);
  req.traceId = incoming || `trace-${crypto.randomUUID()}`;
  res.setHeader('X-Trace-Id', req.traceId);
  next();
});
const LOG_IGNORED_PATHS = new Set(['/health', '/ready']);

app.use(pinoHttp({
  logger,
  autoLogging: { ignore: (req) => LOG_IGNORED_PATHS.has((req.url || '').split('?')[0]) },
  customAttributeKeys: { responseTime: 'durationMs' },
  customLogLevel: (req, res, err) =>
    (err || res.statusCode >= 500) ? 'error' : res.statusCode >= 400 ? 'warn' : 'info',
  customReceivedMessage: (req) => `request received: ${req.method} ${req.originalUrl || req.url}`,
  customSuccessMessage: (req, res) => `request completed: ${req.method} ${req.originalUrl || req.url} -> ${res.statusCode}`,
  customErrorMessage: (req, res) => `request failed: ${req.method} ${req.originalUrl || req.url} -> ${res.statusCode}`,
  serializers: { req: () => undefined, res: (res) => ({ statusCode: res.statusCode }) },
  customProps: (req) => {
    if (req._logPropsBound) return {};
    req._logPropsBound = true;
    const info = clientInfo(req);
    return {
      traceId: req.traceId,
      requestId: info.requestId,
      requestUri: info.endpoint,
      method: req.method,
      clientIp: info.ip,
      browser: info.browser,
      os: info.os,
      device: info.device,
      userAgent: info.userAgent ? info.userAgent.slice(0, 256) : undefined
    };
  }
}));

// --- Kubernetes probes -------------------------------------------------
app.get('/health', (req, res) => res.json({ status: 'ok', service: SERVICE_NAME }));
app.get('/ready', async (req, res) => {
  try {
    await store.ping();
    res.json({ ready: true, service: SERVICE_NAME, storage: store.mode });
  } catch {
    res.status(503).json({ ready: false, service: SERVICE_NAME, storage: store.mode });
  }
});

// --- Consent -------------------------------------------------------------
// POST /consent/:userId  { consentType: "location", granted: true }
// Records/updates the flag. `consentType` defaults to "location" since
// that's the only prompt the app has today — pass it explicitly once a
// second consent type exists.
app.post('/consent/:userId', async (req, res, next) => {
  try {
    const info = clientInfo(req);
    const { userId } = req.params;
    const consentType = String((req.body && req.body.consentType) || 'location');
    const granted = req.body && req.body.granted;

    if (!CONSENT_TYPES.has(consentType)) {
      return res.status(400).json({ error: `Unknown consentType`, details: { consentType } });
    }
    if (typeof granted !== 'boolean') {
      return res.status(400).json({ error: '"granted" must be true or false', details: { granted: 'missing_or_invalid' } });
    }

    const record = await store.upsert(userId, consentType, granted, req.body.source || 'app');

    req.log.info({
      event: 'consent_recorded', userId, consentType, granted,
      ip: info.ip, requestId: info.requestId, device: info.device
    }, 'consent decision recorded');

    // Consent decisions are a compliance-relevant event, same bucket as
    // logins/payments — write one to the shared audit trail on top of the
    // current-state row above.
    audit.record({
      ...info, userId, action: `consent_${consentType}`, success: true,
      statusCode: 200, metadata: { consentType, granted }
    });

    res.json(record);
  } catch (err) { next(err); }
});

// GET /consent/:userId?type=location — current status of one consent type.
// Returns `granted: null` (not a 404) when the user has never been asked,
// so the app can tell "declined" apart from "never prompted" and knows
// whether to show the prompt at all.
app.get('/consent/:userId', async (req, res, next) => {
  try {
    const { userId } = req.params;
    const consentType = String(req.query.type || 'location');
    const record = await store.get(userId, consentType);
    res.json(record || { userId, consentType, granted: null, respondedAt: null });
  } catch (err) { next(err); }
});

// GET /consent/:userId/all — every consent type this user has been asked about.
app.get('/consent/:userId/all', async (req, res, next) => {
  try {
    res.json(await store.listForUser(req.params.userId));
  } catch (err) { next(err); }
});

// --- 404 + error handling ----------------------------------------------
app.use((req, res) => res.status(404).json({ error: 'Route not found' }));
app.use((err, req, res, next) => { // eslint-disable-line no-unused-vars -- Express error signature
  req.log.error({ event: 'unhandled_error', message: err.message }, 'request failed');
  res.status(500).json({ error: 'Internal server error', traceId: req.traceId });
});

function start() {
  const server = app.listen(PORT, () => {
    logger.info({ event: 'service_started', port: PORT, storage: store.mode }, `${SERVICE_NAME} listening`);
    store.init().catch((err) =>
      logger.warn({ event: 'migration_deferred', message: err.message }, 'consent migration will run when the database is up'));
  });
  for (const signal of ['SIGTERM', 'SIGINT']) {
    process.on(signal, () => {
      logger.info({ event: 'shutdown', signal }, 'shutting down gracefully');
      server.close(async () => { if (pool) await pool.end().catch(() => {}); process.exit(0); });
    });
  }
  return server;
}

if (require.main === module) start();

module.exports = { app, store };
