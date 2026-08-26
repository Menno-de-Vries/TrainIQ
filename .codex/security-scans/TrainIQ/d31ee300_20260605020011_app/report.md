# TrainIQ App Security Scan Report

Scope: TrainIQ-Project/app only.
Date: 2026-06-05.

## Result

Three reportable issues were found and fixed:

1. Legacy Gemini plaintext key cleanup was incomplete on explicit encrypted save/clear paths.
2. Health Connect requested/read weight despite TrainIQ's stated Health Connect metric set excluding weight.
3. Reminder notifications could reveal meal/workout behavior on notification surfaces.

No remaining reportable findings after validation.

## Fixes Applied

- AiUsageGate now clears the legacy DataStore Gemini key after successful encrypted save and on Gemini/all-key clear paths.
- Health Connect no longer requests READ_WEIGHT, shows weight in rationale copy, tracks WeightRecord changes, performs full/incremental WeightRecord reads, or includes weight in Health Connect status messages. Manual body-measurement and workout weight models were not removed.
- Reminder notifications now use NotificationCompat.VISIBILITY_PRIVATE and a redacted public notification body.

## Suppressed Findings

- Exported Health Connect rationale/onboarding components: no sensitive data read on launch; privileged aliases are permission-protected.
- Background Health Connect sync: WorkManager-internal and Health Connect permission enforced.
- Import JSON DoS/data loss: UI read cap, import row limits, and Room transaction sink.
- SQL injection: bound Room queries and hardcoded migrations.
- AI API key leakage: headers, no query-string keys, logging disabled.
- Telemetry leakage: disabled by default, opt-in required, allowlist/redaction in place.

## Verification

PASS: `./gradlew.bat :app:testDebugUnitTest --tests com.trainiq.ai.services.AiUsageGateSourceTest --tests com.trainiq.core.reminders.ReminderNotificationPrivacySourceTest --tests com.trainiq.core.health.HealthConnectReadPermissionsTest --tests com.trainiq.data.datasource.HealthConnectPermissionPolicyTest`

PASS: `./gradlew.bat :app:assembleDebug`

PASS: `./gradlew.bat :app:testDebugUnitTest`

PASS: `./gradlew.bat :app:lintDebug`

## Residual Risk

- CachedWeightRecord remains as an internal legacy cache shape for backward-compatible JSON deserialization, but Health Connect code now prunes it to empty and no longer requests or reads WeightRecord.
- Existing unrelated user/worktree changes were preserved.
