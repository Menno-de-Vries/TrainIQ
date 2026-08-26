# Repository Coverage Ledger

| Row | Boundary | Family | Files checked | Disposition | Evidence |
|---|---|---|---|---|---|
| HC-001 | Health Connect permissions | Health data overcollection | AndroidManifest.xml; HealthConnectUiHelpers.kt; HealthConnectDataSource.kt | reportable -> fixed | READ_WEIGHT removed from manifest, permission copy, rationale, tracked records, read paths, status helpers. |
| REM-001 | Notifications | Private health/behavior disclosure | TrainIqReminderNotifications.kt; ReminderPolicy.kt | reportable -> fixed | Notifications now use VISIBILITY_PRIVATE plus redacted public version. |
| KEY-001 | AI key storage | Plaintext legacy secret retention | AiUsageGate.kt; GeminiKeyMigration.kt; UserPreferencesRepository.kt; AndroidKeystore* | reportable -> fixed | Encrypted save/clear paths now clear legacy DataStore key. |
| AI-001 | AI/network clients | Secret in URL/logs | GeminiApi.kt; OpenAiApi.kt; AppModule.kt; AiProviders.kt | suppressed | Keys use headers; OkHttp logging NONE; model JSON bounded and schema-driven. |
| IMP-001 | JSON import | Parser DoS / data loss | SettingsSection.kt; UseCases.kt; JsonRoomImportPlanner.kt; RoomImportDryRun.kt | suppressed | 5 MiB reader cap, row limits, transaction sink. |
| SQL-001 | Room/DAO | SQL injection | TrainIqDao.kt; TrainIqMigrations.kt | suppressed | Room bound parameters; migration SQL hardcoded/internal constants. |
| TEL-001 | Telemetry | Sensitive data export | TelemetryExport.kt; MainActivity.kt; DiagnosticsModule.kt | suppressed | Disabled by default, opt-in, allowlisted attributes, redaction. |
| MAN-001 | Manifest exported components | Component abuse | AndroidManifest.xml; HealthConnectPermissionsRationaleActivity.kt | suppressed | Rationale-only component; privileged aliases protected by permissions. |

All 123 deep_review_input rows were either covered by the grouped runtime-surface reviews above, reviewed as directly supporting context, or excluded as schema/resource/test artifact rows without deployed security boundary.
