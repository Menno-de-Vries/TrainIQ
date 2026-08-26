# TrainIQ QA + Implementatie Loop

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` for inline execution or `superpowers:subagent-driven-development` for task-by-task execution. Track progress with checkbox steps and do not mark findings done without evidence.

**Goal:** Test TrainIQ herhaald op app-functionaliteit, design/UX, accessibility, crashes, ANR's, regressies en ruwe gebruikerservaring; noteer evidence-backed findings; implementeer de kleinste veilige fixes; verifieer gericht; loop door tot de Definition of Done gehaald is.

**Architecture:** Werk binnen TrainIQ's MVVM + Clean Architecture + unidirectional data flow. Business logic blijft in UseCases/repositories, Compose blijft UI/state, ViewModels leveren state en Hilt blijft de DI-grens.

**Tech Stack:** Android, Kotlin, Jetpack Compose, Material 3, Hilt, Room, Health Connect, WorkManager, Retrofit/Gson, CameraX, ML Kit barcode, Gradle, JUnit, Turbine, Android instrumentation tests, adb/uiautomator/logcat.

---

## Scope

Eerste uitvoeringsmodus: `safe emulator/local QA`.

Wel doen:

- Gradle build/test/lint draaien.
- Emulator install/launch smoke uitvoeren.
- UI dumps, screenshots en logcat evidence verzamelen.
- Veilige app-flows testen.
- Reproduceerbare issues noteren.
- Kleine, evidence-backed fixes implementeren.
- Targeted tests toevoegen of aanpassen.
- QA docs, evidence index en loop state bijwerken.

Niet doen zonder expliciete owner approval:

- Echte Gemini/OpenAI keys testen.
- Health Connect permissies muteren.
- TalkBack/Switch Access als PASS sluiten zonder echte traversal.
- Release signing of publishing.
- Secret rotation.
- Destructieve device/account-acties.
- Brede refactors of dependency churn.

Owner-gated items blijven `NOT RUN`, `BLOCKED` of owner-approved `DEFER`.

## Source Of Truth

Lees bij start:

- `AGENTS.md`
- `TrainIQ_Target_State_Blueprint.md`
- `TrainIQ-Project/docs/qa/full-app-qa-run-2026-05-27.md`
- `TrainIQ-Project/docs/qa/qa-status-summary-2026-05-27.md`
- `TrainIQ-Project/docs/qa/fixed-findings-index-2026-05-27.md`
- `TrainIQ-Project/docs/qa/next-run-command-sheet-2026-05-27.md`
- `TrainIQ-Project/docs/qa/release-gate-owner-checklist-2026-05-27.md`
- `.codex/qa-loop-state.md`

Huidige repo-context:

- Automatische baseline was eerder groen.
- Uitgevoerde P0/P1/P2 findings zijn gefixt.
- QA-status blijft `PARTIAL`.
- Release-ready blijft `NO` door owner/runtime-gates.

## Definition Of Done

Safe loop is done wanneer:

- `:app:assembleDebug` PASS is.
- `:app:testDebugUnitTest` PASS is.
- `:app:lintDebug` PASS is.
- `:app:connectedDebugAndroidTest` PASS is als emulator/device beschikbaar is.
- Install/start smoke PASS is.
- Logcat geen TrainIQ crash, ANR of input-dispatch timeout bevat.
- Elke nieuwe safe P0/P1/P2 heeft repro, expected/actual, evidence, fix, targeted verification en regression result.
- Geen nieuwe safe P0/P1/P2 openstaat.
- Owner-gated checks expliciet open of deferred zijn.

Volledige release-DoD mag pas groen wanneer owner-gates echte evidence of approved defer hebben.

## Prioriteit

1. P0: crash, ANR, data loss, privacy/security, kapotte core flow.
2. P1: feature/data correctness, permission/lifecycle, target-state gaps.
3. P2: UX, accessibility, design, touch targets, clipping, focus, readability.
4. P3: cosmetische polish alleen na groen P0-P2.

## Loop

Elke iteratie:

1. Lees huidige QA-status en loop-state.
2. Kies een safe high-risk target.
3. Draai baseline/pre-check als nuttig.
4. Test flow met emulator/local tooling.
5. Verzamel relevante evidence: Gradle output, test output, UI dump/screenshot, logcat.
6. Noteer reproduceerbare issues in QA ledger.
7. Implementeer de kleinste evidence-backed fix batch.
8. Voeg targeted tests toe of pas ze aan.
9. Draai targeted verification.
10. Draai regressiechecks.
11. Update QA docs, evidence index en `.codex/qa-loop-state.md`.
12. Herhaal tot DoD of stopconditie.

## Safe Target Order

Gebruik deze volgorde tenzij docs iets urgenters tonen:

- Current-build smoke + crash/ANR.
- Start/Home first-run.
- Bottom navigation/top-level tabs.
- Settings/Meer.
- Health Connect rationale/no-permission.
- Nutrition forms en recipe create/use/edit/delete.
- Keyboard/IME long forms.
- Training routine, active workout, set edit/delete/undo.
- Exercise History.
- Coach local/fallback flows.
- Camera permission denied/granted.
- Scanner/barcode contracts zonder echte provider.
- Large font, dark mode, touch targets, clipping, focus order, modal containment.

## Finding Format

Elke finding bevat:

- `finding_id`
- `priority`: `P0`, `P1`, `P2` of `P3`
- `area`
- `flow`
- `status`
- evidence paths
- expected behavior
- actual behavior
- repro steps
- recommended fix
- regression risk
- minimal verification
- owner suggestion

Na fix aanvullen:

- changed files
- verification evidence
- regression result
- remaining risk
- status: `fixed`, `partially-done`, `blocked` of `needs-decision`

## Implementatieregels

Volg TrainIQ-standaarden:

- MVVM + Clean Architecture.
- Business logic in UseCases/repositories.
- Compose alleen UI/state.
- ViewModels leveren state.
- Hilt DI respecteren.
- Material 3, Dynamic Color, `MaterialTheme.colorScheme`, `MaterialTheme.typography`.
- Type-safe navigation; geen nieuwe string routes.
- Mapping/data/business logic buiten UI.
- Geen dubbele models/entities/mappers.

Voor datawijzigingen eerst checken:

- `Entities.kt`
- `DomainModels.kt`
- `Mappers.kt`
- repositories
- use cases
- navigation routes

Werk klein, lokaal, testbaar en terugdraaibaar. Revert geen bestaande dirty worktree changes.

## Verificatie

Baseline:

```powershell
cd D:\GitHub\TrainIQ\TrainIQ-Project
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat :app:testDebugUnitTest --console=plain
.\gradlew.bat :app:lintDebug --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

Smoke + logcat:

```powershell
.\gradlew.bat :app:installDebug --console=plain
adb shell pm clear com.trainiq
adb logcat -c
adb shell am start -W -n com.trainiq/.MainActivity
adb shell uiautomator dump /sdcard/trainiq-current-smoke.xml
adb pull /sdcard/trainiq-current-smoke.xml docs/qa/evidence/
adb logcat -d -t 800 > docs/qa/evidence/logcat-current-smoke.txt
Select-String -Path docs/qa/evidence/logcat-current-smoke.txt -Pattern "com.trainiq.*FATAL EXCEPTION|ANR in com.trainiq|Input dispatching timed out.*com.trainiq" -CaseSensitive:$false
```

Targeted verification:

- Run de smalste test die de wijziging bewijst.
- Voeg unit tests toe voor use cases, repositories, mappers en pure policies.
- Voeg instrumented tests toe voor runtime/UI gedrag dat unit tests niet bewijzen.
- Re-run baseline voor loop-afsluiting.

## Result Output Contract

Houd resultaten token-efficient:

- `Result`: PASS/FAIL/PARTIAL + reden.
- `Findings`: alleen nieuwe of gewijzigde findings.
- `Fixes`: korte lijst met files en gedrag.
- `Verification`: command + PASS/FAIL/NOT RUN.
- `Regression`: known regressions yes/no.
- `Remaining risks`: alleen open P0-P2 en owner-gates.
- `Next safest target`: een concrete volgende stap.

Geen lange logs plakken; verwijs naar evidence paths.

## Stopregels

Stop wanneer safe baseline, smoke en logcat groen zijn en geen safe P0/P1/P2 openstaat.

Stop ook bij:

- ontbrekende tooling/device
- vereiste privacy/security/productbeslissing
- owner-gated actie nodig
- regressie die niet veilig opgelost kan worden
