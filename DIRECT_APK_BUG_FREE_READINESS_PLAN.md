# TrainIQ DIRECT_APK Bug-Free Readiness QA Loop V2

## Doel

TrainIQ moet klaar zijn voor normale gebruikers als directe APK-release buiten Google Play.

In dit plan betekent `bug-free`: er zijn geen bekende reproduceerbare P0/P1/P2/P3 bugs meer open uit de uitgevoerde QA-loop, tenzij een risico expliciet door de owner is geaccepteerd als `DEFER`.

Absolute bug-free software kan niet worden gegarandeerd. Deze loop maximaliseert bewijs: elke claim moet terug te vinden zijn in commands, screenshots, UI dumps, logcat, testoutput, QA ledger en statusdocumenten.

## Niet Uitvoeren Tijdens Plan Update

Dit bestand is alleen een control plan. Het vervangen of aanpassen van dit bestand mag de QA-loop niet starten.

Niet uitvoeren tijdens een plan-update:

- Geen Gradle build/test/lint commands.
- Geen adb commands.
- Geen emulator/device install, launch, UI dump, screenshot of logcat.
- Geen Health Connect permissie-mutaties.
- Geen echte AI-provider calls of real-key tests.
- Geen camera/scanner runtime tests.
- Geen release signing credential acties.
- Geen publicatie, upload of externe distributie.
- Geen updates aan QA evidence, ledger, status summary of `.codex/qa-loop-state.md`, behalve wanneer de QA-loop expliciet wordt uitgevoerd.

## Project Context

Repo root:

```powershell
D:\GitHub\TrainIQ
```

Android project:

```powershell
D:\GitHub\TrainIQ\TrainIQ-Project
```

Runtime identifiers:

- App module: `:app`
- Package id: `com.trainiq`
- Launch activity: `com.trainiq/.MainActivity`
- Version source: `TrainIQ-Project\app\build.gradle.kts`

## Source Of Truth

Lees voor elke QA-loop eerst:

- `AGENTS.md`
- `TrainIQ_Target_State_Blueprint.md`
- `TrainIQ-Project/docs/qa/full-app-qa-run-2026-05-27.md`
- `TrainIQ-Project/docs/qa/qa-status-summary-2026-05-27.md`
- `TrainIQ-Project/docs/qa/qa-status-2026-05-27.json`
- `TrainIQ-Project/docs/qa/evidence-index-2026-05-27.md`
- `.codex/qa-loop-state.md`

Gebruik bestaande QA-documenten als source of truth. Maak geen parallelle QA-ledgers tenzij een nieuwe datum/run dat noodzakelijk maakt. Revert geen bestaande dirty worktree changes.

## QA Loop

Elke ronde gebruikt dezelfde cyclus:

```text
Discover -> Reproduce -> Record -> Fix -> Targeted Verify -> Regression Verify -> Update Status -> Repeat
```

Stappen:

1. Lees huidige QA-status, evidence index, full ledger en `.codex/qa-loop-state.md`.
2. Controleer `git status --short` en noteer bestaande wijzigingen die niet van deze loop zijn.
3. Kies het hoogste-risico safe target dat nog open of onvoldoende bewezen is.
4. Draai de kleinste relevante checks voor dat target.
5. Reproduceer de bug of bevestig dat de flow werkt.
6. Verzamel evidence: command output, testoutput, screenshots, UI dumps, logcat en relevante source references.
7. Registreer elke reproduceerbare bug met finding-id, priority, repro, expected, actual en evidence.
8. Fix alleen de kleinste veilige batch die direct bij de finding hoort.
9. Voeg of update regression tests waar dat zinvol en onderhoudbaar is.
10. Draai targeted verification die exact de fix bewijst.
11. Draai bredere regression verification voordat de loop sluit.
12. Update QA ledger, evidence index, QA status summary en `.codex/qa-loop-state.md`.
13. Herhaal tot de Definition of Done gehaald is of een owner-gated blocker overblijft.

## Prioriteiten

P0 blockers:

- App start niet.
- Crash.
- ANR.
- Dataverlies.
- Privacy/security-lek.
- Release APK bouwt, installeert of start niet.

P1 blockers:

- Kapotte kernflow.
- Persistence bug.
- Room migration bug.
- Permission/lifecycle bug.
- Health Connect contract kapot.
- AI-key, AI-provider of AI-fallback flow kapot.
- Scanner/camera permission flow kapot.
- Navigatie of back behavior blokkeert normaal gebruik.

P2 blockers:

- Storende UX bug.
- Accessibility probleem dat normale bediening hindert.
- Tekst clipping of overlap.
- Touch target onder veilige maat.
- Onjuiste loading, empty of error state.
- Modal/dialog/bottom sheet is onduidelijk, overlapt of moeilijk te sluiten.
- Large-font, dark-mode of focus issue dat vertrouwen schaadt.

P3 blockers:

- Zichtbare polish issues.
- Inconsistente copy.
- Kleine designfouten die normale gebruikers duidelijk zien.
- Niet-blokkerende layout- of feedbackproblemen.

## Deep Test Matrix

Functionaliteit:

- Startup, splash en cold launch.
- Top-level navigatie en back behavior.
- Start/Home.
- Training.
- Active Workout.
- Exercise History.
- Voeding.
- Coach.
- Voortgang.
- Settings/Meer.
- Health Connect rationale, no-permission, provider-missing en status states.
- Camera/scanner permission denied/granted states.
- AI fallback, missing-key, invalid-key en provider states.

Backend en data:

- Room entities, schemas en migrations.
- DAO/repository transacties.
- UseCases.
- Mappers.
- Persistence na restart.
- Active workout draft/session persistence.
- Nutrition meal/recipe/product history.
- Local data clear.
- Encrypted AI key storage.
- Import/export waar relevant.

Frontend en design:

- Material 3 theming.
- Dynamic Color.
- Dark mode.
- Font scale `1.3` en `1.5`.
- Tekst clipping.
- Layout overlap.
- Modal, dialog en bottom sheet containment.
- Tablet/foldable smoke waar beschikbaar.
- Loading, empty, error en retry states.

Accessibility:

- Content descriptions.
- Semantics labels.
- Touch targets van minimaal 48dp.
- Focusable nodes.
- Dialog pane titles.
- Focus order.
- Large-font behavior.
- TalkBack/Switch Access alleen als werkelijk uitgevoerd of owner-approved deferred.

Android runtime:

- Fresh install.
- Cold launch.
- Background/foreground.
- Lock/unlock.
- Rotation.
- Process/activity recreation.
- Upgrade install met geldige signing lineage.
- Crash/ANR/input-timeout logcat scan.

Release APK:

- Release signing readiness.
- Release build.
- Install from APK.
- Launch from APK.
- Release smoke.
- Geen secrets in artifacts, UI dumps of logs.

## Baseline Commands

Voer commands uit vanuit:

```powershell
cd D:\GitHub\TrainIQ\TrainIQ-Project
```

Automated baseline:

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat :app:testDebugUnitTest --console=plain
.\gradlew.bat :app:lintDebug --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

Release APK readiness:

```powershell
.\gradlew.bat :app:checkReleaseSigningReadiness --console=plain
.\gradlew.bat :app:assembleRelease --console=plain
```

## Runtime Smoke Commands

Debug smoke example:

```powershell
cd D:\GitHub\TrainIQ\TrainIQ-Project
.\gradlew.bat :app:installDebug --console=plain
adb shell pm clear com.trainiq
adb logcat -c
adb shell am start -W -n com.trainiq/.MainActivity
adb shell uiautomator dump /sdcard/trainiq-current-smoke.xml
adb pull /sdcard/trainiq-current-smoke.xml docs/qa/evidence/
adb logcat -d -t 1500 > docs/qa/evidence/logcat-current-smoke.txt
Select-String -Path docs/qa/evidence/logcat-current-smoke.txt -Pattern "com.trainiq.*FATAL EXCEPTION|ANR in com.trainiq|Input dispatching timed out.*com.trainiq" -CaseSensitive:$false
```

Release smoke example:

```powershell
cd D:\GitHub\TrainIQ\TrainIQ-Project
.\gradlew.bat :app:installRelease --console=plain
adb logcat -c
adb shell am start -W -n com.trainiq/.MainActivity
adb shell uiautomator dump /sdcard/trainiq-release-smoke.xml
adb pull /sdcard/trainiq-release-smoke.xml docs/qa/evidence/
adb logcat -d -t 1500 > docs/qa/evidence/logcat-release-smoke.txt
Select-String -Path docs/qa/evidence/logcat-release-smoke.txt -Pattern "com.trainiq.*FATAL EXCEPTION|ANR in com.trainiq|Input dispatching timed out.*com.trainiq" -CaseSensitive:$false
```

Screenshots mogen worden toegevoegd waar UI, clipping, visual state, modal containment of accessibility evidence nodig is.

## Upgrade Gate

Gebruik debug-to-release install niet als geldige upgrade-proof. Debug en release builds hebben normaal verschillende signatures en kunnen `INSTALL_FAILED_UPDATE_INCOMPATIBLE` geven zonder dat dit een app bug is.

Geldige upgrade readiness vereist:

- een eerder geinstalleerde release APK;
- een nieuwe release APK;
- dezelfde package id `com.trainiq`;
- compatibele signing lineage;
- install over bestaande release;
- launch na upgrade;
- persistence check waar relevant;
- logcat crash/ANR scan.

Als dezelfde release signing lineage niet beschikbaar is, markeer de upgrade gate als `NOT RUN` met exacte reden of als owner-approved `DEFER`.

## Finding Format

Elke finding gebruikt dit format:

```markdown
## Finding QA-YYYY-MM-DD-###

- priority: P0 | P1 | P2 | P3
- area:
- flow:
- status: open | fixed | partially-fixed | blocked | needs-decision | owner-deferred
- repro steps:
- expected behavior:
- actual behavior:
- evidence paths:
- recommended fix:
- regression risk:
- changed files:
- targeted verification:
- regression verification:
- remaining risk:
- final status:
```

Regels:

- Geen finding zonder reproduceerbare stappen of duidelijke evidence.
- Geen `fixed` status zonder targeted verification.
- Geen release-ready claim als een P0/P1 openstaat.
- Geen owner-gated check als `PASS` markeren zonder echte uitvoering of expliciete owner-approved defer.

## Implementatieregels

Volg TrainIQ engineering standards:

- MVVM + Clean Architecture + unidirectional data flow.
- Business logic staat in UseCases/repositories.
- UI gebruikt state uit ViewModels.
- Elke screen heeft een duidelijke `uiState`.
- Hilt DI respecteren.
- Repositories blijven `@Singleton` waar van toepassing.
- ViewModel-afhankelijke objecten blijven `@ViewModelScoped` waar van toepassing.
- Type-safe navigation gebruiken; geen nieuwe string-based routes.
- Material 3 gebruiken via `MaterialTheme.colorScheme` en `MaterialTheme.typography`.
- Dynamic Color blijven ondersteunen op Android 12+.
- Geen mapping, persistence of business logic in de UI-laag.
- Geen dubbele entities, domain models, mappers of repositories toevoegen.

Voor datawijzigingen altijd eerst controleren:

- `TrainIQ-Project/app/src/main/java/com/trainiq/core/database/Entities.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/domain/model/DomainModels.kt`
- `TrainIQ-Project/app/src/main/java/com/trainiq/data/mapper/Mappers.kt`
- repositories
- use cases
- navigation routes

Werk klein, lokaal, testbaar en terugdraaibaar. Revert geen bestaande wijzigingen die niet door de lopende QA-fix zijn gemaakt.

## Owner-Gated Checks

Deze checks worden alleen uitgevoerd met expliciete toestemming of benodigde middelen:

- Health Connect permission mutation matrix: partial grant, revoke-while-open en background-read.
- Echte AI-provider calls met echte keys.
- Real-key save/readback/privacy signoff.
- TalkBack/Switch Access volledige traversal.
- Release signing credentials beheren of wijzigen.
- Publiceren, uploaden of externe distributie.
- Destructieve device/account-acties.

Als deze checks niet uitgevoerd zijn, blijven ze zichtbaar als `NOT RUN`, `blocked`, `needs-decision` of owner-approved `DEFER`. Ze mogen niet stilzwijgend als `PASS` worden gemarkeerd.

## Definition Of Done

`Direct APK Ready: YES` mag alleen worden gezet wanneer alle vereiste automated, runtime, release, documentation en owner-gated checks `PASS` zijn of expliciet owner-approved als `DEFER`.

Automated gates:

- `:app:assembleDebug` is `PASS`.
- `:app:testDebugUnitTest` is `PASS`.
- `:app:lintDebug` is `PASS`.
- `:app:connectedDebugAndroidTest` is `PASS` wanneer emulator/device beschikbaar is.
- `:app:connectedDebugAndroidTest` is alleen `NOT RUN` als exacte device/tooling reden is genoteerd.
- `:app:checkReleaseSigningReadiness` is `PASS` of owner-resolved.
- `:app:assembleRelease` is `PASS`.

Runtime gates:

- Fresh install is `PASS`.
- Cold launch is `PASS`.
- Release APK install is `PASS`.
- Release APK launch is `PASS`.
- Logcat scan toont geen `FATAL EXCEPTION`, TrainIQ ANR of input dispatch timeout.
- Top-level app navigatie is getest.
- Alle core user flows zijn getest of expliciet owner-approved deferred.

Data en functional gates:

- Geen bekende open reproduceerbare P0/P1/P2/P3 bugs blijven over, tenzij expliciet owner-accepted.
- Geen bekende data-loss bugs blijven open.
- Geen bekende broken persistence bugs blijven open.
- Geen bekende Room migration bugs blijven open.
- Geen bekende Health Connect contract bugs blijven open.
- Geen bekende AI fallback/provider bugs blijven open.
- Geen bekende scanner/camera permission bugs blijven open.
- Geen bekende broken-navigation bugs blijven open.

UX/accessibility gates:

- Geen blocking clipping, overlap, modal containment, touch-target, focus, dark-mode, large-font of screen-reader issues blijven open uit uitgevoerde checks.
- TalkBack/Switch Access blijft `NOT RUN` tenzij werkelijk getraversed of owner-approved deferred.
- Large-font en dark-mode findings hebben evidence en regressiechecks.

Documentation gates:

- QA ledger is bijgewerkt.
- Evidence index is bijgewerkt.
- QA status summary is bijgewerkt.
- `.codex/qa-loop-state.md` is bijgewerkt.
- Eindrapport bevat `Direct APK Ready: YES` of `Direct APK Ready: NO`.
- Eindrapport bevat build id, device/emulator, commands met `PASS`/`FAIL`/`NOT RUN`, findings, fixes, open risks, evidence paths en next action.

Stop conditions:

- Als een P0/P1 openstaat, eindstatus is altijd `Direct APK Ready: NO`.
- Als alleen owner-gated checks openstaan, eindstatus blijft `Direct APK Ready: NO` tenzij owner-approved defer is vastgelegd.
- Als tooling/device ontbreekt, noteer exacte reden, next best check en resterend risico.
- Als een fix regressie veroorzaakt, open een nieuwe finding en loop opnieuw.

## Documentatie Die Bijgewerkt Moet Worden Tijdens QA Execution

Bij echte QA-loop execution moeten deze bestanden worden bijgewerkt:

- `TrainIQ-Project/docs/qa/full-app-qa-run-2026-05-27.md`
- `TrainIQ-Project/docs/qa/qa-status-summary-2026-05-27.md`
- `TrainIQ-Project/docs/qa/qa-status-2026-05-27.json`
- `TrainIQ-Project/docs/qa/evidence-index-2026-05-27.md`
- `.codex/qa-loop-state.md`

Evidence hoort onder:

```text
TrainIQ-Project/docs/qa/evidence/
```

Gebruik een datum- en target-specifieke submap per loop.

## Final Report Contract

Het eindrapport van een uitgevoerde loop is kort en evidence-backed:

- `Direct APK Ready: YES` of `Direct APK Ready: NO`.
- Laatste commit/build identifier.
- App version/build id.
- Device/emulator.
- Commands met `PASS`, `FAIL` of `NOT RUN`.
- Nieuwe of gewijzigde findings.
- Gefixte bugs.
- Regression result.
- Open P0/P1/P2/P3 bugs.
- Owner-gated risks.
- Evidence paths.
- Concrete next action als readiness nog `NO` is.

Geen lange logs plakken; verwijs naar evidence paths.
