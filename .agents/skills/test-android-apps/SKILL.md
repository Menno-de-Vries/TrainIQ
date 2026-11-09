---
name: test-android-apps
description: "Use for Android build/test/emulator/device QA, Gradle diagnosis, adb/logcat review, lifecycle, permissions, accessibility, performance, crash checks, and screenshot smoke checks."
---

# Test Android Apps

AGENTS.md and docs/agent-guides/local-testing.md own test selection, safety and canonical commands. Read them once; this skill adds no second checklist or mandatory report.

1. Identify changed behavior and transitive risk, then select its cheapest reliable proof. Docs/skills-only work needs content/frontmatter/link checks, not Android execution.
2. Run focused tests while editing. Batch one integration baseline when warranted: :app:assembleDebug, :app:testDebugUnitTest, :app:lintDebug and relevant Android-test compilation. Do not use generic test tasks or rerun all gates for each agent/commit.
3. Add canonical :app:connectedDebugAndroidTest only for affected Android, Room, navigation, lifecycle or semantics boundaries. Reuse uninvalidated evidence; retain failing regression tests and diagnose failures before retrying.
4. Use one known isolated serial. Verify ownership/boot, scope every adb call, preserve user data, and use installed tooling. For temporary display changes on an owned AVD include restoration in the same bounded operation.
5. Inspect changed UI using representative states and the smallest useful viewport/font/IME/theme matrix, with screenshots plus semantics. Avoid every-state cross products; record fixture versus actual app boundaries.
6. Record source, exact commands, counts/results and evidence paths once. Capture only necessary logs/images. Emulator evidence is not physical performance, heard audio or human TalkBack evidence.

For APK update proof, finish instrumentation first (the runner can uninstall the target). Then verify the exact old APK/signature, install the exact final APK with install -r and prove selected data preservation. Do not run instrumentation afterwards.
