---
name: android-quality-gate
description: Use when validating Android app quality, lifecycle, permissions, accessibility, emulator/device behavior, Gradle builds, tests, adb/logcat, and release-readiness.
---

# Android Quality Gate

Use test-android-apps (../test-android-apps/SKILL.md) and docs/agent-guides/local-testing.md as the single verification workflow. Do not launch a duplicate gate because both skills were selected.

Select only risks changed by this task: state/lifecycle, navigation, Room, permissions, remote boundaries, accessibility or performance. Preserve required happy-path, boundary and failure/recovery proof at the lowest reliable layer. Reuse valid prior results. Docs-only edits do not require an emulator/build; a device being available is not by itself a reason to run it. Report exact results and evidence limits, without formal certification claims.
