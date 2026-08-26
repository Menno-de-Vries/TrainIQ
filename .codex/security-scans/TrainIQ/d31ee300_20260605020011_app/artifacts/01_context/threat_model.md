# TrainIQ Repository Threat Model

Scope source: TrainIQ Android health app engineering standards and local app code.

## Assets
- Health Connect-derived activity, sleep, heart-rate, calories, workout, and cached sync-token data.
- User-entered profile, nutrition, body-measurement, workout, reminder, and progress data.
- User-provided Gemini/OpenAI API keys.
- Local Room database, DataStore preferences, encrypted key SharedPreferences, import/export JSON, diagnostics/telemetry payloads.

## Trust Boundaries
- Android app process vs. external apps launching exported activities or document pickers.
- Health Connect provider and system permissions vs. app-local cache and background sync.
- User-provided JSON import/export documents vs. Room import planner and transaction sink.
- User-provided AI keys and user prompts/images vs. outbound Gemini/OpenAI/Barcode/telemetry clients.
- Lock-screen/notification surfaces vs. private app UI.

## Attacker-Controlled Inputs
- Documents selected for TrainIQ JSON import.
- Barcode values, AI prompts/scanned image-derived model output, settings form values, and URI/document picker inputs.
- External app launches of exported manifest components.
- Local device adversary observing notifications, backups, logs, or plaintext preferences.

## Invariants
- Health data permissions must be least-privilege and match TrainIQ standards.
- Secrets must not remain in plaintext legacy storage after encrypted storage succeeds.
- Notification/public surfaces must not reveal sensitive meal/workout behavior.
- Imports must be bounded, validated, and transactional.
- Network calls must avoid cleartext secrets and unsafe logging.
