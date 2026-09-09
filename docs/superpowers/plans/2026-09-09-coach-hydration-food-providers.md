# Coach, hydration and food providers implementation plan

Goal: Move body progress into Coach, persist explicit hydration under Nutrition, and add Open Food Facts-first provider selection with a server-side FatSecret adapter.

Architecture: Preserve the typed Progress route and existing measurement repository. Room remains authoritative and device-local (the app has no remote user/tenant store). Hydration requires explicit volume, a unique nutrition-log relationship and transactional edits/deletes. Provider responses must remain separate, with bounded requests and no client secrets.

User specification: the task attachment and subsequent instruction authorize the existing task branch/PR workflow and a locally testable backend, with no deployment or secret changes.

- [x] Establish unchanged baseline: assembleDebug, testDebugUnitTest, lintDebug.
- [x] Move Progress access from Settings into Coach; update selection policy and test affected navigation and actual UI routes.
- [x] Add explicit-volume hydration domain/persistence with non-destructive migration, unique relationship, edit/delete/retry contracts and import/export handling.
- [x] Add Nutrition hydration UI and explicit drink volume editing; verify semantics, restoration, empty/error/save states.
- [x] Normalize provider contracts and barcode validation; test selection/fallback/error distinctions.
- [x] Add local server adapter with OAuth token expiry/cache, bounded transport, configured localization and official storage/attribution restrictions.
- [x] Integrate compact source selection and provenance into existing lookup flows only.
- [x] Run affected JVM, connected migration/UI and local baseline gates; inspect diff and secrets; commit exact task paths and create one PR to origin/main.

Risk: high for persistence invariants and provider unit/storage semantics; medium for navigation. Never infer ml from grams or backfill historical logs. Live FatSecret access remains unverified until account entitlements and a gateway are available.
