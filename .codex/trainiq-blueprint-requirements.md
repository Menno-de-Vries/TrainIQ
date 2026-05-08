# TrainIQ Blueprint Requirements Matrix

| ID | Blueprint section | Requirement | Current evidence | Status | Files/modules | Validation |
|---|---|---|---|---|---|---|
| PV-01 | Product Vision | AI-native health coach with safe passive data, combined coaching, bounded AI, resilient layouts, and reliable manual fallback. | Core flows exist; release evidence still depends on performance/accessibility/owner gates. | partial | app, docs | build/tests pass; release QA partial |
| BASE-01 | Current QA Baseline | Use `TrainIQ_Target_State_Blueprint.md` as canonical target file. | Exact file found and read. | satisfied | repo root | matrix created |
| GAP-HIGH-01 | Prioritized Findings | Room runtime mutations must not leave stale rows through upsert-only mirror imports. | Runtime update now passes `RoomMirrorImportRun`, causing transactional stale-row clearing. | satisfied | RoomTrainIqRuntimeStore.kt | unit guard, app tests |
| GAP-HIGH-02 | Prioritized Findings | Startup/first draw must be bounded by profileable/release macrobenchmark. | Profileable launch avoids timeout, but jank remains and physical benchmark is unavailable. | partial | macrobenchmark, MainActivity | adb launch, macrobenchmark blocked on emulator |
| GAP-HIGH-03 | Prioritized Findings | Active workout logging and meal saves must use bounded targeted writes. | Stale-row correctness fixed through full replacement; hot paths still serialize/import full state. | partial | repositories/DAO | unit tests pass; targeted DAO work remains |
| GAP-HIGH-04 | Prioritized Findings | Top-level traversal must meet performance thresholds on release-like and lower-end physical device. | Macrobenchmark exists and compiles; physical evidence unavailable. | partial | macrobenchmark | emulator run not release-valid |
| GAP-MED-01 | Prioritized Findings | Health Connect permission UX supports independent permissions and partial success. | Partial result copy implemented and tested. | satisfied | HealthConnectUiHelpers.kt, rationale activity | unit tests |
| GAP-MED-02 | Prioritized Findings | Critical flows pass compact/large-font proof. | Prior artifacts exist; current full manual matrix not rerun. | partial | UI screens/docs QA | connected tests; manual QA blocked |
| GAP-MED-03 | Prioritized Findings | Workout completion returns immediately with local summary; Gemini refreshes async. | Local debrief saved before async Gemini launch. | satisfied | TrainIqRepository.kt | unit architecture guard |
| GAP-MED-04 | Prioritized Findings | Production AI boundary decided and documented. | Decision docs exist but owner decision remains open. | blocked | docs/architecture, docs/security | owner signoff required |
| ARCH-01 | Architecture | Room/Health/Remote -> Repositories -> UseCases -> ViewModels -> Compose. | Existing architecture and tests cover this. | satisfied | UseCases, repositories, screens | architecture tests |
| ARCH-02 | Architecture | Business logic outside composables; one sealed `uiState`. | Existing architecture tests and ViewModels support this. | satisfied | feature ViewModels | unit tests |
| ARCH-03 | Architecture | Singleton repositories and scoped helpers where needed. | Hilt bindings present. | satisfied | AppModule | compile/Hilt |
| ARCH-04 | Architecture | Navigation 2.8+ type-safe routes only. | Serializable routes and nav tests exist. | satisfied | TrainIqNav.kt | nav tests |
| ARCH-05 | Architecture | No UI DB mapping; coordinator decomposition continues. | Several collaborators exist, but coordinator remains large. | partial | TrainIqRepository.kt | architecture tests |
| DATA-01 | Data/Room | Room source of truth; DataStore limited. | Room runtime store and architecture guards present. | satisfied | RoomTrainIqRuntimeStore.kt | unit tests |
| DATA-02 | Data/Room | Auto/SQL migrations and exported schemas. | v12 schema and migrations exist. | satisfied | database, schemas | migration tests |
| DATA-03 | Data/Room | Foreign keys and indexed child columns. | Existing v12 work covers this. | satisfied | Entities.kt | connected migration tests |
| DATA-04 | Data/Room | Inserts/updates/deletes transactional; delete/discard cannot resurrect. | Full replacement prevents stale rows; targeted delete proof remains incomplete for all flows. | partial | runtime store, DAO | unit/connected tests |
| DATA-05 | Data/Room | Dirty migration tests and FK checks. | Connected tests and marker generation pass. | satisfied | TrainIqDatabaseMigrationTest.kt | connected tests, marker |
| DATA-06 | Data/Room | Field additions update entities/domain/mappers/repos/use cases/tests. | No new Room/domain fields added in this pass. | satisfied | n/a | review |
| HC-01 | Health Connect | Required metrics: steps, heart rate, sleep, active calories, weight, workouts. | Permissions and sync code include these. | satisfied | HealthConnectDataSource.kt | unit tests |
| HC-02 | Health Connect | SDK status, unsupported/provider update, rationale before prompt. | Existing implementation and tests cover this. | satisfied | health core | unit/instrumented tests |
| HC-03 | Health Connect | Full/partial/denied states; sync granted metrics. | Data policy plus UX copy supports partial. | satisfied | HealthConnectUiHelpers.kt, datasource | unit tests |
| HC-04 | Health Connect | Per-metric status/tokens, pagination, aggregates. | Existing datasource tests cover policy. | satisfied | HealthConnectDataSource.kt | unit tests |
| HC-05 | Health Connect | Token expiry and missing permission isolated per metric. | Existing policy tests cover token isolation. | satisfied | datasource | unit tests |
| HC-06 | Health Connect | Background sync bounded and gated. | Existing worker policy tests cover this. | satisfied | HealthConnectBackgroundSyncWorker.kt | unit tests |
| HC-07 | Health Connect | Play Console declaration/Data Safety/privacy parity. | Docs exist; external owner submission needed. | blocked | docs/release | owner action |
| AI-01 | Gemini/AI | Stable `gemini-2.5-flash`, no latest aliases. | Existing AI services use stable model. | satisfied | AiServices.kt | unit tests |
| AI-02 | Gemini/AI | Fast/deep thinking budgets and JSON MIME. | Existing schemas/configs cover this. | satisfied | AI services | unit tests |
| AI-03 | Gemini/AI | Structured JSON schemas; no regex extraction. | Existing parser/schema tests. | satisfied | GeminiJsonSchemas.kt | unit tests |
| AI-04 | Gemini/AI | Schema contract tests for variants. | Existing AI tests cover schema parsing cases. | satisfied | AI tests | unit tests |
| AI-05 | Gemini/AI | No chain-of-thought; consistent coach persona. | Prompt/service contracts use summaries. | satisfied | GeminiPrompts.kt | unit tests/review |
| AI-06 | Gemini/AI | Timeout/retry/cancel/fallback/429 handling. | Bounded retry/fallback exists. | satisfied | AiSupport.kt | unit tests |
| AI-07 | Gemini/AI | Image bounds, header auth, Keystore key storage. | Existing implementation/tests cover key/header policy. | satisfied | GeminiApi, security | unit tests |
| AI-08 | Gemini/AI | User-initiated AI; production should prefer gateway/OAuth over BYOK. | BYOK is implemented; production mode not decided. | partial | docs/security, settings | owner decision |
| UI-01 | UI/UX | Material 3, theme, dynamic color, shimmer, animation, haptics, adaptive. | Existing UI and tests cover core standards. | satisfied | theme/UI | lint/tests |
| UI-02 | UI/UX | No clipping, 48dp, Dutch copy, first-class states. | Existing UI tests/docs and lint pass. | satisfied | screens | lint/tests |
| UI-03 | UI/UX | Compact navigation policy with hidden destination paths/tests. | Existing adaptive nav tests and settings path. | satisfied | TrainIqNav.kt | nav tests |
| UI-04 | UI/UX | Dialog/sheet actions reachable on compact/large-font/landscape. | Some tests/artifacts exist; full matrix not rerun. | partial | screens | manual QA needed |
| UI-05 | UI/UX | Edge-to-edge/insets tested across modes. | Edge-to-edge exists; full gesture/landscape/freeform matrix not complete. | partial | MainActivity/UI | manual QA needed |
| UI-06 | UI/UX | Automated/manual accessibility for dense flows. | Scripts exist; human TalkBack/Switch Access signoff not done. | blocked | docs/qa | human QA required |
| FEAT-01 | Feature State | Home cockpit, next action, graceful degradation, no startup sync/AI block. | Existing behavior and launch checks. | satisfied | HomeScreen | tests/adb |
| FEAT-02 | Feature State | Training resilient, active state preserved, bounded persistence, idempotent finish, async debrief. | Async debrief fixed; bounded targeted persistence remains partial. | partial | workout/repository | tests |
| FEAT-03 | Feature State | Nutrition manual logging and scan failure states. | Existing tests; full camera/offline/manual matrix not rerun. | partial | NutritionScreen | unit/manual partial |
| FEAT-04 | Feature State | Barcode target explicit. | Existing scanner/product flow present; full offline/not-found verification not current. | partial | NutritionScreen | manual partial |
| FEAT-05 | Feature State | Coach grounded in data, declares quality, validates JSON. | Existing AI/data-quality tests. | satisfied | Coach/AI | unit tests |
| FEAT-06 | Feature State | Progress separates trends/data quality/body/performance/recovery. | Existing model/UI support. | satisfied | ProgressScreen | unit tests |
| FEAT-07 | Feature State | Settings controls Health, AI, privacy, telemetry, theme, profile/local data; confirms destructive actions. | Existing settings tests/docs. | satisfied | SettingsSection.kt | unit/manual tests |
| PERF-01 | Performance | No heavy main-thread/startup sync/full serialization critical paths. | Startup better profileable, but full-state mutation remains. | partial | runtime/repositories | adb/unit |
| PERF-02 | Performance | Benchmarks/profile evidence and thresholds for critical journeys. | Macrobenchmark/producers exist; physical thresholds missing. | partial | macrobenchmark/docs | emulator not valid |
| PERF-03 | Performance | `am start -W` no timeout; 90%+ jank unacceptable. | Profileable launch no timeout; jank warnings remain. | partial | app runtime | adb/logcat |
| PERF-04 | Performance | Generated baseline profiles each release train with critical coverage. | BaselineProfileRule added and generated under suppressed emulator log; release workflow not fully device-backed. | partial | macrobenchmark | compile/logcat |
| SEC-01 | Backend/Security/DX | Header auth, typed failures, timeout/retry, safe errors, production AI signoff. | Runtime safeguards exist; AI signoff open. | partial | AI/docs | tests/owner |
| SEC-02 | Backend/Security/DX | No secrets in BuildConfig, plaintext keys, URL keys, static telemetry tokens, sensitive logs. | Existing and current checks support this. | satisfied | build/security | unit/review |
| SEC-03 | Backend/Security/DX | Telemetry opt-in, bounded flush, local redacted diagnostics, production docs. | Local telemetry docs/code exist; production endpoint/auth/retention owner work remains. | partial | diagnostics/docs | tests/docs |
| DX-01 | Backend/Security/DX | CI on PR/protected push; README covers setup. | CI triggers added; README already covers setup. | satisfied | .github, README | review |
| DX-02 | Backend/Security/DX | Stable dependencies and Room update evaluation. | Stable HC used; broader dependency/Room evaluation remains future pass. | partial | Gradle/docs | review |
| DX-03 | Backend/Security/DX | Migration marker in CI/release, release shrinking, Room gate. | Shrinking enabled and marker generated; CI device marker gate remains partial. | partial | Gradle/CI | build/marker |
| TEST-01 | Test Strategy | Minimum `:app:assembleDebug`. | Passed. | satisfied | Gradle | assembleDebug |
| TEST-02 | Test Strategy | Required area tests including mutation resurrection and hot-path persistence. | Broad tests pass; hot-path benchmark/guard remains partial. | partial | tests | unit/connected |
| TEST-03 | Test Strategy | Full exploratory release QA matrix. | Cannot complete without manual/physical/device owner work. | blocked | docs/qa | manual QA required |
| DOD-01 | Definition of Done | Complete only when architecture, states, compile/tests, performance, compact, state, partial HC/disabled AI, privacy all verified. | All feasible local checks pass; non-blocked implementation improved; release-only evidence remains. | partial | all | this matrix |
