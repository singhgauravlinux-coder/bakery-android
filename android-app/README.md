# Crumb & Ember — Android app

A native Android (Kotlin + Jetpack Compose) client for the `bakery-android`
platform. It talks to the same `api-gateway` the web storefront (`services/frontend`)
uses — nothing on the backend changes. Postgres, Redis, and all 23 services keep
running exactly as they do today (Docker Compose locally, or the Kubernetes/Argo CD
setup in `k8s/` + `argocd/` for real deployments).

```
Android app (this project)
        │  HTTPS, JSON, Bearer JWT
        ▼
   api-gateway  (services/api-gateway, port 3000)
        │  proxies /api/<segment>/... to the owning service
        ▼
auth-service · product-catalog-service · cart-service · order-service · ...
```

## What's implemented

The core commerce flow end-to-end, matching what a customer does on the storefront:

- **Auth** — register, login, JWT access/refresh token storage (DataStore), automatic
  silent refresh on a 401, logout. (`auth-service`)
- **Catalog** — product list, category-aware, search box. (`product-catalog-service`,
  `search-service`)
- **Product detail** — add to cart with a quantity stepper.
- **Cart** — view/remove items, running total. (`cart-service`)
- **Checkout** — choose card / UPI / cash-on-pickup, places a real order.
  (`order-service`)
- **Order confirmation & history** — look up any order by id, list all of a user's
  orders.
- **Profile** — shows the signed-in user, logout, and current location-access status.
- **Location consent** — a dedicated screen shown once, right after login, if the person
  has never been asked; revisit anytime from Account > Location access. (`consent-service`)

## What's not wired up yet

The gateway exposes 53 endpoints across 23 services; this app covers the ones a
storefront customer touches. Natural next additions, following the same
Repository → ViewModel → Compose-screen pattern already established:

- **Payments** — `/api/payments/razorpay/order` + `/verify` for real card/UPI
  charges (currently checkout only records the order; it doesn't run a payment
  gateway flow).
- **Reviews, recommendations, loyalty, delivery tracking, promotions/currency,
  language** — each is a small addition: one `ApiService` method, one repository
  function, one screen.
- **Push notifications** — `notification-service` already sends email; wiring FCM
  for order-status pushes would replace/augment that.
- **Product images** — `media-service` serves images; `ProductCard`/detail screen
  don't render any yet (Coil is already a dependency, ready for an `AsyncImage`
  once you have image URLs from `media-service`).

## Environments (build flavors)

There's one Gradle **product flavor per Argo CD-managed environment**, each hardcoded to
the ingress host that environment already serves (see `../k8s/overlays/<env>/patch-ingress-
host.yaml` and `../argocd/<env>-app.yaml` in the backend repo). Picking a flavor is the only
configuration step — nothing else changes per environment.

| Flavor | `API_BASE_URL` | Application ID | Matches |
|---|---|---|---|
| `dev` | `http://10.0.2.2:3000/api/` (emulator → local `docker compose`) | `com.crumbandember.app.dev` | `bakery-dev` namespace / `dev.bakery.local` once you point it there |
| `uat` | `http://uat.bakery.local/api/` | `com.crumbandember.app.uat` | `bakery-uat` namespace |
| `production` | `https://bakery.local/api/` | `com.crumbandember.app` | `bakery-prod` namespace |

`dev`/`uat`/`production` can all be installed side by side on one device, exactly like the
backend's three namespaces coexist in one cluster — handy for testing a `uat` promotion
without losing your `dev` build.

Cleartext HTTP is only permitted for `10.0.2.2`, `dev.bakery.local`, and `uat.bakery.local`
(`app/src/main/res/xml/network_security_config.xml`); anything else, including a
misconfigured build, fails closed instead of silently allowing plaintext.

## Outage handling

Every network call is classified, not just labeled "failed" (`util/Resource.kt`,
`ErrorKind`), so the UI can show the right full-screen state instead of one generic
"something went wrong":

| `ErrorKind` | When | What the user sees |
|---|---|---|
| `OFFLINE` | Device has no network at all | "You're offline — check your connection" |
| `GATEWAY_DOWN` | Device is online, but `api-gateway` itself never responded (connect/timeout/DNS failure) | "Our app server is down" — a whole-app problem, not one feature |
| `SERVICE_UNAVAILABLE` | Gateway responded, but the specific microservice behind this screen returned 502/503/504 | "We're having trouble with `<feature>`. We're on it — please try again in a moment." The rest of the app keeps working. |
| `SERVER_ERROR` | Gateway's own 500 | "Something went wrong on our end" |
| `UNAUTHORIZED` / `NOT_FOUND` / `UNKNOWN` | Everything else | Existing small inline error — not worth a full screen |

`ui/components/UnavailableScreens.kt` has one composable per state (`GatewayDownScreen`,
`ServiceUnavailableScreen`, `OfflineScreen`, `ServerErrorScreen`) plus a single
`UnavailableScreen(kind, featureName, onRetry)` dispatcher that every load-bearing screen
(Catalog, Product detail, Cart, Order history, Order detail) calls instead of a bare error
message. Action-triggering screens (Login, Register, Checkout) use the lighter
`friendlyInlineMessage(kind, message)` helper instead, so a failed submit doesn't wipe out
what the person already typed into the form.

`util/AppNetworkState`, initialized once in `BakeryApplication.onCreate`, is what
distinguishes "phone has no network" from "phone's fine, the gateway isn't answering" —
both throw the same `IOException` from OkHttp, and only a `ConnectivityManager` check tells
them apart.



1. Start the backend as usual:
   ```bash
   docker compose up --build -d
   ```
2. Open this folder (`android-app/`) in Android Studio (Koala+ / AGP 8.5). Let it
   sync — Android Studio will offer to generate the Gradle wrapper jar if it's
   missing.
3. Pick the **dev** build variant (Build Variants panel, or
   `./gradlew installDevDebug`) and run on an emulator — `10.0.2.2` reaches the
   gateway started by `docker compose` above with zero extra config.
4. Log in with the seeded demo account: `amelie@crumbandember.dev` / `baguette`,
   or register a new one.

### Testing against uat or a real cluster

Switch the Build Variant to **uat** or **production** (or
`./gradlew installUatDebug` / `installProductionDebug`) — no code or config
edit needed, since each flavor already points at that environment's real
ingress host. This only works once `uat.bakery.local` / `bakery.local`
resolve for your device (cluster DNS, `/etc/hosts`, or a VPN/port-forward),
same as the web storefront.

### CI/CD

`../.github/workflows/android-app.yml` + `_mobile-pipeline.yml` build and test
this app on every push, then gate `uat`/`production` release builds behind the
same GitHub Environment reviewers the backend's services already use — see
`../docs/DEPLOYMENT.md#the-mobile-apps-place-in-this-pipeline` for the full
picture of how mobile and backend promotions line up.

## Location consent

`ui/screens/consent/LocationConsentScreen.kt` separates two different yes/no's, on purpose:

1. **Does the person consent** — recorded in `consent-service`'s `user_consents` table via
   `ConsentRepository`. This is what "we asked, they said yes" means for compliance
   purposes, and it survives reinstalls/device changes since it lives server-side, not in
   `SharedPreferences`.
2. **Does Android grant the runtime permission** — `ACCESS_FINE_LOCATION`, requested via
   `ActivityResultContracts.RequestPermission()` only *after* the person says yes on (1).
   Android can revoke this in system Settings at any time without telling the app, so the
   two can drift; that's fine, (1) is the source of truth for "were they asked and what did
   they say," not "is the OS permission live right now."

Saying "Not now" skips the OS dialog entirely — no point interrupting someone who already
said no. If Android denies the permission after the person tapped "Allow," the saved
consent flag is corrected back to `false` so the two can't end up silently contradicting
each other.

Shown once, automatically, right after login, only if `GET /consent/:userId` comes back
with `granted: null` (never asked). Revisit the decision anytime from **Account > Location
access**, which shows live status (Allowed / Not allowed / Not asked yet) and re-opens the
same screen.



```
app/src/main/java/com/crumbandember/app/
├── BakeryApplication.kt       # manual DI: wires TokenManager, ApiService, repositories
├── MainActivity.kt
├── data/
│   ├── api/                   # Retrofit interface + OkHttp/auth-refresh setup
│   ├── local/                 # DataStore-backed token storage
│   ├── model/                 # DTOs matching each service's actual JSON shape
│   └── repository/            # one repo per domain (auth/product/cart/order)
├── ui/
│   ├── navigation/            # NavHost + route definitions
│   ├── screens/{auth,catalog,cart,checkout,orders,profile}/
│   ├── components/            # shared composables (ProductCard, error/loading states)
│   └── theme/                 # ember color palette, typography
└── util/                      # Resource<T> wrapper, ViewModel factory helper
```

## Notes on design choices

- **Manual DI, not Hilt** — kept the whole wiring visible in `BakeryApplication.kt`
  for a project this size. Swap in Hilt/Koin if the app grows.
- **Cart line prices are resolved client-side** — `cart-service` only stores
  `{productId, quantity}`; `CartViewModel`/`CheckoutViewModel` fetch each
  product from `product-catalog-service` to get price/name for display and to
  compute the order total, mirroring what the storefront's JS does.
- **Token refresh** lives in an OkHttp `Authenticator`, so it applies uniformly
  to every request without each repository needing to know about it.
