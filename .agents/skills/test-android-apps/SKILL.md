---
name: test-android-apps
description: "Use for Android build/test/emulator/device QA, Gradle diagnosis, adb/logcat review, lifecycle, permissions, accessibility, performance, crash checks, and screenshot smoke checks."
---

# Test Android Apps

Use this skill when validating Android app quality, release readiness, emulator behavior, or device-specific risks.

## Workflow

1. Detect modules, variants, package id, min/target SDK, and test tasks.
2. Run the smallest relevant Gradle checks first.
3. If a device or emulator is available, install and launch the app.
4. Inspect lifecycle, permissions, navigation, loading/error/offline states, and crash risk.
5. Capture adb logcat crash evidence when launch is possible.
6. Check accessibility basics: labels, touch targets, font scaling risk, dark mode, and TalkBack semantics where inspectable.
7. Record exact PASS, FAIL, or NOT RUN reasons for every check.

Do not install external dependencies unless the repository already uses them.
