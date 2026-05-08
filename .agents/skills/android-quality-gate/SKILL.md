---
name: android-quality-gate
description: Use when validating Android app quality, lifecycle, permissions, accessibility, emulator/device behavior, Gradle builds, tests, adb/logcat, and release-readiness.
---

Use this skill to:
- check Gradle/build health;
- check Android lifecycle, configuration changes, process death risk, and state preservation;
- check permissions, privacy, storage/media access, network behavior, and background behavior where relevant;
- check navigation, loading states, empty states, error states, offline states, and retry behavior;
- check responsive layouts, dark mode, font scaling, touch targets, TalkBack labels, and accessibility semantics;
- check startup, crash risk, ANR risk, jank risk, memory risk, and severe logcat warnings;
- run the smallest relevant checks first;
- if emulator/device access exists, launch the app, exercise core flows, inspect adb logcat, and capture screenshots if useful;
- report exact commands and PASS/FAIL/not run with reason.
