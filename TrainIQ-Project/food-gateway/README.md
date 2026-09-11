# Local food gateway

Run `node --test food-gateway/fatsecret.test.mjs` from `TrainIQ-Project` (Node 22+; no packages required). Start with `node food-gateway/server.mjs`. The server binds only to `127.0.0.1:8787`; `PORT` optionally changes the port. Nothing is deployed by these commands.

Server-only environment names: `FATSECRET_CLIENT_ID`, `FATSECRET_CLIENT_SECRET`, optional `FATSECRET_SCOPES` (default `barcode`), `FATSECRET_REGION`, `FATSECRET_LANGUAGE`. Configure region/language only with granted localization scope. Tokens are single-flight, memory-only, expire early and never go to the app. No product cache or request/response logging is used.

The Android build accepts the non-secret Gradle property `trainiq.foodGatewayUrl` for an HTTPS gateway URL. It defaults to empty and reports unavailable configuration safely. Do not embed OAuth credentials or a shared gateway bearer secret into the APK. This local adapter has no production authentication/rate-limiting boundary: deployment requires a separately authorized gateway with HTTPS, user authentication and quota controls. No app health history is sent; the lookup request contains only a barcode.

FatSecret's standard storable-data guidance permits indefinite storage of identifiers, not nutrient payloads. TrainIQ's existing local diary stores nutrient snapshots. Therefore the HTTP adapter fails closed unless `FATSECRET_DURABLE_NUTRITION_ALLOWED=true` is set **after obtaining/verifying account-specific rights for that use**. This is not a purchase or activation instruction. No such rights have been assumed or configured in this task. The pure adapter is testable with synthetic fixtures without credentials.

The existing product model represents nutrients per 100 grams. The adapter selects only explicit gram-based servings; ml/oz-only results return `unsupported_serving` rather than fabricating a density. Products are normalized from one provider only. EAN-8, UPC-A and EAN-13 are checksum-validated and padded to GTIN-13.

Official sources checked 2026-09-09:

- [FatSecret barcode v2](https://platform.fatsecret.com/docs/v2/food.find_id_for_barcode): GET path, scope, GTIN-13, code 211, localization.
- [OAuth 2](https://platform.fatsecret.com/docs/guides/authentication/oauth2): server-side client credentials, token endpoint, IP allowlisting and scopes.
- [Storable data](https://platform.fatsecret.com/docs/guides/storable-data): retention restrictions. Verify account-specific license and attribution before enabling live access.
- [Open Food Facts API](https://openfoodfacts.github.io/openfoodfacts-server/api/): identifying User-Agent, bounded product lookups (currently 15/min/IP), ODbL attribution/license. Existing OFF product lookup remains primary; no new search modality or lookup cache is added.

Remaining live-access requirements: account credentials/entitlements and permitted nutrient retention, an authorized secure gateway deployment and its non-secret Android URL. No live access, deployment, account changes or purchases were performed.
