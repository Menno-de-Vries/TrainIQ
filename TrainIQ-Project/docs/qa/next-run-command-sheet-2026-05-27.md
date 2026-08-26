# TrainIQ Next QA Run Command Sheet - 2026-05-27

Use this sheet for the next owner/runtime pass. It does not close any current `NOT RUN` row by itself.

## Baseline before new changes

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat :app:testDebugUnitTest --console=plain
.\gradlew.bat :app:lintDebug --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

## Current-build smoke

```powershell
$adb = "C:\Users\menno\AppData\Local\Android\Sdk\platform-tools\adb.exe"
.\gradlew.bat :app:installDebug --console=plain
& $adb shell pm clear com.trainiq
& $adb logcat -c
& $adb shell am start -W -n com.trainiq/.MainActivity
Start-Sleep -Seconds 3
& $adb shell uiautomator dump /sdcard/trainiq-current-smoke.xml
& $adb pull /sdcard/trainiq-current-smoke.xml docs/qa/evidence/
& $adb logcat -d -t 800
```

Logcat failure patterns:

```powershell
Select-String -Path <logcat-file> -Pattern "com.trainiq.*FATAL EXCEPTION|ANR in com.trainiq|Input dispatching timed out.*com.trainiq" -CaseSensitive:$false
```

## Physical-device macrobenchmark

Run only on a real device:

```powershell
$adb = "C:\Users\menno\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb devices
& $adb shell getprop ro.kernel.qemu
& $adb shell getprop ro.product.model
.\gradlew.bat :app:assembleProfileable :macrobenchmark:assembleProfileable --console=plain
.\gradlew.bat :macrobenchmark:connectedProfileableAndroidTest --console=plain
```

## Health Connect runtime mutation pass

Capture state before and after each permission mutation:

```powershell
$adb = "C:\Users\menno\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb logcat -c
& $adb shell am start -W -n com.trainiq/.MainActivity
& $adb shell uiautomator dump /sdcard/trainiq-healthconnect-state.xml
& $adb pull /sdcard/trainiq-healthconnect-state.xml docs/qa/evidence/
& $adb logcat -d -t 800
```

Required cases:
- No permission/rationale.
- Partial permission grant.
- Revoke while app is open.
- Background-read available and denied.

## TalkBack/Switch Access pass

Before traversal:

```powershell
$adb = "C:\Users\menno\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb shell settings get secure accessibility_enabled
& $adb shell settings get secure enabled_accessibility_services
& $adb shell pm list packages | Select-String "accessibility|talkback|switch"
```

During traversal, capture each high-risk flow:

```powershell
& $adb shell uiautomator dump /sdcard/trainiq-a11y-flow.xml
& $adb pull /sdcard/trainiq-a11y-flow.xml docs/qa/evidence/
& $adb exec-out screencap -p > docs/qa/evidence/<run-dir>/trainiq-a11y-flow.png
& $adb logcat -d -t 800
```

## Privacy/security real-key pass

Use only approved throwaway keys. After save/delete/local-clear:

```powershell
rg -n --hidden --glob '!**/.gradle/**' --glob '!**/build/**' --glob '!**/.git/**' "AIza[0-9A-Za-z_-]{20,}|sk-[A-Za-z0-9_-]{20,}|sk-proj-[A-Za-z0-9_-]{20,}|OPENAI_API_KEY\s*=|GEMINI_API_KEY\s*=" .
```

Expected result: no raw key matches.

## Contract-only rescans

These do not replace runtime proof, but are useful after code changes:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.ai.services.AiServicesTest" --tests "com.trainiq.ai.services.RoutineGeneratorServiceTest" --tests "com.trainiq.features.coach.GoalAdviceInputTest" --console=plain
.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.navigation.ScannerModeRouteTest" --tests "com.trainiq.features.nutrition.CameraScannerStateTest" --tests "com.trainiq.features.nutrition.CameraUiStateMapperTest" --tests "com.trainiq.data.remote.BarcodeProductLookupServiceTest" --tests "com.trainiq.features.nutrition.NutritionInputValidationTest" --console=plain
.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.health.*" --tests "com.trainiq.data.datasource.HealthConnectPermissionPolicyTest" --console=plain
.\gradlew.bat :app:testDebugUnitTest --tests "com.trainiq.core.ui.AppDialogAccessibilityTest" --tests "com.trainiq.core.ui.LineChartSemanticsTest" --tests "com.trainiq.features.nutrition.CameraScannerStateTest" --tests "com.trainiq.features.workout.WorkoutInputValidationTest" --tests "com.trainiq.features.settings.SettingsUiStateTest" --console=plain
```
