---
name: trainiq-apk-drive-email
description: "Build the current remote TrainIQ main as a signed APK, validate and ZIP it, upload to the existing private TrainIQ Drive folder, and email the verified link on explicit invocation."
---

# TrainIQ APK to Drive and email

An explicit invocation authorizes one delivery through the user's existing signing, Google Drive and email accounts. Use the exact folder name **TrainIQ**. Do not merge, cherry-pick, change main, publish to Play/itch.io, deploy, expand sharing, or create signing material. A request to inspect or dry-run this skill authorizes no distribution.

## Ground the run

Read the repository's AGENTS.md, local testing guide and current release policy, including `TrainIQ-Project/docs/release/itch-release-policy.md`. Inspect the official Gradle/signing configuration and relevant release evidence. Honor existing user authorization; do not invent additional approval gates.

Discover the primary checkout using Git's common directory and worktree inventory. Preserve unrelated work. Inspect remotes and the existing task branch/PR. Fetch the verified remote's main without checking out or modifying local main. Record the full `refs/remotes/<remote>/main` SHA once; do not silently advance it during retries. Current remote main can already contain previously merged work; report this fact rather than choosing an older commit or merging anything yourself.

Use a detached, clean worktree at that exact SHA. Record its path, primary checkout, remote and SHA. Never copy modified app sources from a task branch into the build worktree. Keep skill edits on the authorized task branch. If its PR is already merged, explain that new commits cannot be added to it and establish the intended PR disposition.

## Resume ledger and duplicate prevention

Before any upload or email, create or reopen an **ignored, local-only** ledger under the primary checkout's `.codex/device-qa/apk-deliveries/`. Check ignore status before writing. Use one run key derived from repository identity, pinned main SHA and build variant. A retry resumes that run; it does not create a new run or fetch a new build SHA.

Record only operational evidence: SHA, paths, variant/version, check results, APK/ZIP names, sizes and hashes, resolved destination/account identifiers, mutation attempt states, returned Drive file URL/ID and email message ID. Never record tokens, passwords, keystore contents or signed download credentials. Do not commit this ledger, APKs, ZIPs or logs.

Use exclusive local file creation/locking for an active run; another invocation must not deliver concurrently. Preserve a stale/ambiguous lock for reconciliation rather than blindly removing it. Save the ledger atomically after each state change:

`pinned -> built -> validated -> packaged -> upload_pending -> uploaded -> link_verified -> email_pending -> sent`

- Write `upload_pending` with exact parent/name/hash **before** the upload call. Search the resolved folder for that exact name first. Reuse a proven matching file. A same-name/different-content or ambiguous match is a blocker, not permission to overwrite or upload again.
- If an upload times out, reconcile by folder/name and checksum, or authenticated download/hash. Do not repeat a create call while its outcome is unknown.
- Write `email_pending` with recipient, deterministic subject and Drive ID before sending. Search Sent for that subject, SHA and link first. Record and reuse a matching sent message.
- A send timeout requires Sent reconciliation; no match in an eventually consistent search is not proof of non-delivery. Leave the email step blocked if the outcome remains ambiguous. Do not resend unless separately requested.
- A completed run reports its existing link/message instead of distributing again. An explicit resend request authorizes only the requested resend, not another upload/build.

## Build and validate locally

Discover Java/SDK from existing configuration and standard installed locations. Set command-scoped environment variables. Use only signing material already available to the official release configuration; never print secret values, pass passwords on a command line, copy credentials into the worktree, or substitute debug signing for a distributable release.

From the clean worktree's `TrainIQ-Project/`:

1. Run `:app:checkReleaseSigningReadiness`. If unavailable/incomplete, preserve independent progress and report signing as blocked; do not label a debug APK as a release.
2. Run the existing release APK task (`:app:assembleRelease`) and the applicable local gates from the testing guide. Inspect available tasks: this project uses `:app:testDebugUnitTest` for its unit suite; do not assume `testReleaseUnitTest` exists. Select the actual lint/build/install tasks for the pinned configuration. Generate migration evidence only through the configured test/marker tasks when required; never fabricate or copy stale generated markers.
3. Check build exit codes, clean tracked status and unchanged HEAD. Record versionName/versionCode and application ID from the produced APK/AGP output metadata. Do not infer provenance from a filename alone.
4. With installed Android tools, run `apksigner verify --verbose --print-certs`, `aapt dump badging` (or equivalent APK analyzer) and `zipalign -c` using the alignment supported by the build. Require nonzero size, a valid APK archive/manifest, expected package/version, release variant and a valid existing release signature. Record SHA-256, signer digest and exact tool results.
5. Install/start the exact release APK on a known isolated agent-owned AVD where available, with every adb command scoped to its serial. If a different signing certificate is already installed, do not uninstall or erase data without separate authorization. Use another safe target or report installation validation blocked. Never reset an unknown physical device. Check launch/activity and app-specific fatal logs. Run affected release smoke routes where relevant. Do not claim physical-device performance from an emulator.

Unrelated gate failures require explicit baseline evidence and an accurate delivery decision. Task-related failures must be resolved without modifying the pinned source. If that requires changing main, stop distribution and report the defect instead of silently shipping another revision.

## Package

Use a name such as `TrainIQ-<version>-main-<sha12>-release.apk` and the same stem for `.zip`. Put artifacts in an ignored local distribution directory outside tracked source. Package **only that APK**, unless the existing distribution convention requires another specific file. Never zip a build directory.

Reopen the ZIP, assert the exact entry allowlist and no directory traversal names, read/decompress each entry and compare the embedded APK's size/SHA-256 to the validated APK. Record ZIP size/SHA-256. On resume, verify an existing ZIP and reuse it rather than rewriting it and changing its hash.

## Drive and email

Discover the currently callable Google Drive and email connectors and inspect their schemas. Use connected Drive search/metadata for grounding. An upload action must support the local artifact/file reference; follow its current input schema rather than exposing base64 or credentials. Never replace the APK ZIP with a native Google document. Browser fallback is acceptable only through an authorized account and supported UI tools.

Resolve the existing non-trashed Drive **folder** named exactly `TrainIQ`; inspect parent/account metadata to disambiguate duplicates. Do not create another folder when the destination exists. If ambiguity remains, ask for the destination. Resolve the email recipient from the explicit request or a reliable prior TrainIQ delivery plus current authorized profile. A Git author address or an arbitrary contact is not recipient authority. Never hardcode an email, Drive ID or account ID in this skill or its scripts.

Check/reconcile the ledger and remote files before uploading the ZIP once. Verify returned success, exact parent, name, MIME type and size; compare a server checksum or authenticated bytes when available. Obtain the actual observed Drive web link through upload/metadata readback. Check access through the authenticated account and inspect permission metadata. Preserve existing private/inherited access; do not create public/anyone links or add grants.

If Gmail and Drive are different accounts belonging to the requester, a private link may require signing into the connected Drive owner account. Explain that in the email and report the account used for link validation. Do not claim that the Gmail identity independently has Drive access. For another recipient lacking existing access, stop the email step and request the missing access decision; do not widen permissions yourself.

Only after `link_verified`, send **one** actual email using the existing connector. Include the TrainIQ version, full main SHA, release variant and verified Drive link, plus any relevant installation/account limitation. Use a deterministic subject containing the SHA. Record the returned message ID and confirm Sent metadata. A draft is not a sent message. If recipient/email access is missing, retain the verified upload and report only the remaining email step as blocked.

## Report and dry-run

Report pinned main SHA, build variant/version, APK/ZIP names and hashes, build/artifact/install results, observed Drive file/link and access scope, email status/message ID, and concrete remaining blockers. Report any retained worktree/owned emulator. Commit/push/PR changes only when authorized; never merge or clean up someone else's work. No credentials or private artifact contents belong in the report.

For a dry-run: inspect instructions, configuration and available tools; validate skill frontmatter/UI metadata and walk through ledger cases without building/uploading/sending unless separately requested. Test at least fresh delivery, completed resume, upload timeout, send timeout, missing signing and missing recipient. Use existing distribution evidence; never send a second delivery merely to test this skill.
