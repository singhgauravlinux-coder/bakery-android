# Crumb & Ember — Complete Stack & Deployment Process

_Exported from the `bakery-android` platform, including the native Android client
added on top of it. This document is the single reference for what the system is made of and
how a change gets from a laptop to production._

---

## 1. System overview

```
┌─────────────────────────────┐       ┌──────────────────────────────┐
│   Android app (Kotlin/       │       │   Web storefront               │
│   Jetpack Compose)            │       │   (services/frontend, nginx)  │
└──────────────┬───────────────┘       └───────────────┬──────────────┘
               │              HTTPS / JSON / Bearer JWT               │
               └───────────────────────┬───────────────────────────────┘
                                        ▼
                          ┌─────────────────────────┐
                          │      api-gateway          │
                          │  proxies /api/<segment>   │
                          └──────────────┬────────────┘
                                         │
        ┌──────────────┬────────────────┼────────────────┬──────────────┐
        ▼              ▼                ▼                ▼              ▼
  auth-service   product-catalog   cart-service    order-service   payment-service
  user-service   inventory         pricing         delivery        notification(+worker)
  review         search            recommendation  promotion       loyalty
  recipe         baking-schedule   supplier         analytics       media
  invoice        currency          language
        │              │                │                │              │
        └──────────────┴────────────────┴────────────────┴──────────────┘
                                         ▼
                         ┌───────────────────────────┐
                         │  postgres   ·   redis        │
                         └───────────────────────────┘

  notification-service / notification-worker ──▶ mailpit (dev SMTP sink)
  adminer ──▶ postgres (dev-only DB admin UI)
```

**Totals:** 1 API gateway + 26 backend microservices + 1 web frontend + Postgres + Redis +
2 dev-only tools (Mailpit, Adminer) + 1 native Android client.

---

## 2. Backend services (Docker Compose service names)

| Domain | Service |
|---|---|
| Edge | `api-gateway` |
| Identity | `auth-service`, `user-service` |
| Catalog & pricing | `product-catalog-service`, `inventory-service`, `pricing-service`, `search-service`, `recommendation-service` |
| Commerce | `cart-service`, `order-service`, `payment-service`, `promotion-service`, `loyalty-service`, `invoice-service` |
| Fulfilment | `delivery-service`, `baking-schedule-service`, `supplier-service`, `recipe-service` |
| Engagement | `review-service`, `notification-service`, `notification-worker` |
| Platform | `media-service`, `analytics-service`, `currency-service`, `language-service`, `consent-service` |
| Data | `postgres`, `redis` |
| Dev tooling | `adminer` (DB UI), `mailpit` (SMTP sink) |
| Client | `frontend` (web storefront, nginx) |

Full definitions: `docker-compose.yml`. Each service also has its own Dockerfile under
`services/<name>/`.

---

## 3. Mobile client — Android app

Native Kotlin + Jetpack Compose app (`android-app/`) that talks to the same `api-gateway` the
web storefront uses. Backend is untouched by its existence.

**Screens:** Login/Register → Catalog (search) → Product detail → Cart → Checkout
(card/UPI/cash) → Order confirmation → Order history → Profile/logout.

**Architecture:**
- `data/api` — Retrofit `ApiService` + OkHttp client with JWT auth header injection and
  automatic 401 → `/auth/refresh` retry
- `data/local` — DataStore-backed token storage
- `data/repository` — one repo per domain (auth/product/cart/order)
- `ui/screens/*` — one ViewModel + Compose screen per domain
- `ui/theme` — brand palette matching the storefront (Espresso `#3B2417`, Ember orange
  `#D9752B`, Butter `#F6E7C9`, Cream `#FFF8EE`, Crust brown `#7A5233`)

**Environments (Gradle product flavors)** — each hardcoded to the ingress host its matching
Argo CD Application already serves, so picking a flavor is the only configuration step:

| Flavor | `API_BASE_URL` | App ID | Matches |
|---|---|---|---|
| `dev` | `http://10.0.2.2:3000/api/` (emulator → local compose) | `com.crumbandember.app.dev` | `bakery-dev` |
| `uat` | `http://uat.bakery.local/api/` | `com.crumbandember.app.uat` | `bakery-uat` |
| `production` | `https://bakery.local/api/` | `com.crumbandember.app` | `bakery-prod` |

Cleartext HTTP is allow-listed only for `10.0.2.2`, `dev.bakery.local`, `uat.bakery.local`
(`network_security_config.xml`) — production is HTTPS-only by construction.

---

## 4. Local development

```bash
git clone <this repo>
cd bakery-android
docker compose up --build -d
```

- Storefront: `http://localhost:8080` (via `frontend`)
- API gateway: `http://localhost:3000/api`
- Mailpit (catches all outbound email): `http://localhost:8025`
- Adminer (Postgres UI): `http://localhost:8081`
- Android app: open `android-app/` in Android Studio, run the **dev** variant on an emulator —
  `10.0.2.2` reaches the compose stack above with no config.

---

## 5. Kubernetes layout

```
k8s/
├── base/
│   ├── services/       # 26 Deployment+Service manifests, one per microservice
│   ├── data/           # Postgres + Redis StatefulSets/Deployments
│   └── ingress/        # single Ingress: /api → api-gateway, / → frontend
└── overlays/
    ├── dev/            # patches: host = dev.bakery.local
    ├── uat/             # patches: host = uat.bakery.local
    └── production/      # full ingress override: host = bakery.local
```

Kustomize builds each overlay: `kubectl kustomize k8s/overlays/<env>`.

---

## 6. Continuous delivery — Argo CD

Three `Application` resources (`argocd/{dev,uat,production}-app.yaml`), one per overlay:

| Environment | Namespace | Sync policy | Notes |
|---|---|---|---|
| `bakery-dev` | `bakery-dev` | **automated** (prune + selfHeal) | Ships the moment CI commits a new image tag — no human in the loop |
| `bakery-uat` | `bakery-uat` | automated, gated upstream by CI reviewers | Only receives a commit after the "uat" GitHub Environment approves |
| `bakery-production` | `bakery-prod` | **no automated block** | A human runs `argocd app sync bakery-production` deliberately |

Register once per cluster:
```bash
kubectl apply -f argocd/dev-app.yaml -n argocd
kubectl apply -f argocd/uat-app.yaml -n argocd
kubectl apply -f argocd/production-app.yaml -n argocd
```

Mobile parity is documented inline in each of these files: the Android flavor for that
environment points at the exact same ingress host the Application serves.

---

## 7. CI/CD pipelines (GitHub Actions)

### Backend — one workflow per service + a shared reusable pipeline

```
.github/workflows/
├── _service-pipeline.yml     # reusable: build image → push GHCR → bump tag in overlay
├── api-gateway.yml            # thin caller, path-filtered to services/api-gateway/**
├── auth-service.yml            # ...one per service, 26 total
└── ... (25 service callers)
```

Promotion flow per service, driven by `_service-pipeline.yml`:

1. **Build & push** — every push to `main` touching that service's path builds a Docker image
   and pushes it to GHCR, tagged with the short SHA.
2. **Bump dev** — commits the new tag into `k8s/overlays/dev`, no gate. Argo CD's `bakery-dev`
   auto-syncs it within its poll interval.
3. **Bump uat** — same, but the job sits behind the **"uat" GitHub Environment**'s required
   reviewers.
4. **Bump production** — behind the **"production" GitHub Environment**'s required reviewers;
   the commit lands, but `bakery-production`'s Argo CD Application still needs a manual
   `argocd app sync`.

### Mobile — one workflow + a shared reusable pipeline

```
.github/workflows/
├── _mobile-pipeline.yml     # reusable: build+test → promote-uat → promote-production
└── android-app.yml           # thin caller, path-filtered to android-app/**
```

| Stage | What happens | Gate |
|---|---|---|
| Build & test | Unit tests + lint + `assembleDevDebug`, uploaded as a build artifact | none — every push/PR |
| Promote uat | `assembleUatRelease`, attached to a `mobile-uat-<sha>` GitHub pre-release | "uat" GitHub Environment reviewers (same group as backend) |
| Promote production | `bundleProductionRelease`, attached to a `mobile-production-<sha>` GitHub release | "production" GitHub Environment reviewers, then a human still uploads the `.aab` to Play Console |

Release signing reads `ANDROID_KEYSTORE_BASE64` / `ANDROID_KEYSTORE_PASSWORD` /
`ANDROID_KEY_ALIAS` / `ANDROID_KEY_PASSWORD` from the same `uat`/`production` GitHub
Environments — add them as Environment secrets to switch from debug-signed to properly signed
builds with no other change.

**Important boundary:** Argo CD reconciles Kubernetes resources only. A compiled APK/AAB is
not a cluster resource, so it is never added as a fake Argo CD Application — what mobile and
backend share is the *review policy* (same reviewer groups, same dev/uat/production shape,
same "one more manual step" before production), not the deployment mechanism itself.

---

## 8. End-to-end promotion example

Shipping an order-service fix and a matching mobile update together:

1. Push a fix to `services/order-service/**` → `order-service.yml` builds/pushes an image,
   bumps `k8s/overlays/dev` → `bakery-dev` auto-syncs.
2. Push the matching UI change to `android-app/**` → `android-app.yml` builds/tests, uploads
   a dev APK artifact.
3. Once verified in dev, a reviewer approves both the backend's "uat" bump job and the
   mobile's `promote-uat` job (same "uat" Environment) → `bakery-uat` gets the new image,
   a `mobile-uat-<sha>` release gets the new APK. Both now point at `uat.bakery.local`.
4. QA signs off in uat. A reviewer approves both "production" gates.
5. A human runs `argocd app sync bakery-production` for the backend, and uploads the
   `mobile-production-<sha>` `.aab` to Play Console for the app. Both changes are now live
   against `bakery.local`.

---

## 9. Reference: file locations

| What | Where |
|---|---|
| Local dev orchestration | `docker-compose.yml` |
| Per-service source | `services/<name>/` |
| Gateway routing table | `services/api-gateway/server.js` (`UPSTREAMS`), `openapi.js` |
| K8s manifests | `k8s/base/`, `k8s/overlays/{dev,uat,production}/` |
| Argo CD apps | `argocd/{dev,uat,production}-app.yaml` |
| Backend CI | `.github/workflows/_service-pipeline.yml` + 26 per-service callers |
| Mobile app | `android-app/` |
| Mobile CI | `.github/workflows/_mobile-pipeline.yml`, `android-app.yml` |
| Full deployment writeup | `docs/DEPLOYMENT.md` |
