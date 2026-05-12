# TrainIQ Flagship Phone Promo Verification

Date: 2026-05-12

## Output

- Final video: `D:\GitHub\TrainIQ\promo\2026-05-flagship-phone\trainiq-flagship-phone-promo-2026-05.mp4`
- Size: 10,727,510 bytes
- Duration: 27.05 seconds
- MP4 tracks: `vide`, `soun`
- Format: vertical 1080x1920, 30fps Remotion composition

## Source Captures

Fresh emulator captures were taken from installed package `com.trainiq` on `emulator-5554`.

- Initial emulator state had `wm density` override `160`, causing tablet/foldable navigation rail.
- Corrected to phone profile with `adb shell wm density 420`.
- Final screenshots use compact phone layout with bottom navigation.

Screenshots:

- `public/screenshots/01-home-dashboard.png`
- `public/screenshots/02-training-plan.png`
- `public/screenshots/03-nutrition.png`
- `public/screenshots/05-coach.png`
- `public/screenshots/06-settings-health-connect.png`
- `public/screenshots/07-active-workout.png`

UIAutomator XML evidence is stored next to the captures under `evidence/*.xml`.

## Generated Asset

- Generated decorative background: `public/generated/premium-health-backdrop.png`
- Prompt intent: premium abstract health-and-strength coaching background for Remotion.
- Constraint followed: generated asset is decorative only and does not show fake app UI.

## Commands Run

- `C:/Users/menno/AppData/Local/Android/Sdk/platform-tools/adb.exe devices`
- `.\gradlew.bat :app:assembleDebug :app:installDebug --console=plain --no-configuration-cache`
- `npm.cmd install`
- `npm.cmd run audio`
- `npm.cmd run still`
- `npx.cmd remotion still src/index.ts TrainIqFlagshipPromo evidence/still-frame-420.png --frame=420 --scale=0.35`
- `npm.cmd run render`

## Known Blocker / Deviation

- `:app:installDebug` failed with `INSTALL_FAILED_INSUFFICIENT_STORAGE`; `/data` had about 360M available.
- Because `com.trainiq` was already installed and launchable, captures were taken from that installed app instead of reinstalling the freshly built APK.
- `ffmpeg` was not found on PATH, but Remotion rendered the H.264/AAC MP4 successfully through its compositor.

## Visual Checks

- `evidence/still-frame-120.png`: early scene check after intro/caption cleanup.
- `evidence/still-frame-420.png`: middle scene check with phone-frame, real app screenshot, generated backdrop, and readable caption.
