# TrainIQ Privacy Policy Draft

> Release scope update (2026-09-06): [itch.io release policy](itch-release-policy.md) governs current delivery. The formal owner gates below are retired for itch.io; Play submission checklists are future reference material. Preserve accurate privacy and security descriptions.

Last updated: 2026-05-12

Status: draft for owner/legal review. Every legal claim requires `OWNER_CONFIRMATION_REQUIRED` before publication.

Current release status: `BLOCKED`. Review this draft with `docs/release/owner-decision-packet-2026-05-10.md` (content refreshed on 2026-05-12), `docs/release/owner-action-tracker.md`, and `docs/release/play-console-owner-checklist.md`. Do not publish until production AI, telemetry, Health Connect, signing/versioning, and Data Safety decisions are final.

## Overview

TrainIQ helps users log training and nutrition and, when users choose, combine local app data with Health Connect data and AI features from Google Gemini or OpenAI.

OWNER_CONFIRMATION_REQUIRED: confirm publisher legal name, contact email, jurisdiction, effective date, and policy URL.

## Data We Process

TrainIQ may store the following data locally on the device:

- Profile and goal data such as name/profile label, age, biological sex, height, weight, body-fat percentage, activity level, and goal.
- Training data such as routines, exercises, sets, workout sessions, workout notes, and active workout state.
- Nutrition data such as meals, foods, recipes, ingredients, macro estimates, and notes.
- Progress measurements such as body measurements and trend values.
- Photos selected for meal analysis or smart-scale/body-measurement reading; check local retention and temporary-file behavior before publication.
- Health Connect data after permission is granted: steps, heart rate, sleep, active calories, and exercise sessions.
- Health Connect cache metadata such as sync tokens and last sync timestamps.
- Google Gemini and OpenAI API keys if the user chooses Bring Your Own Key AI. These keys are stored locally using Android Keystore-backed encryption.
- Technical diagnostics and performance summaries only if telemetry is enabled and configured.

OWNER_CONFIRMATION_REQUIRED: confirm final data categories against Play Console Data Safety wording.

## Health Connect

TrainIQ requests Health Connect access only after showing an explanation screen and the Android system permission flow. Users can grant or deny individual data types. TrainIQ should continue to work manually when Health Connect is unavailable or denied.

TrainIQ reads Health Connect data for activity, recovery, progress, and coaching context. Health Connect permissions can be changed or revoked in Android Health Connect settings.

OWNER_CONFIRMATION_REQUIRED: confirm exact Health Connect declaration and whether background reads are enabled in production.

## AI Providers and Bring Your Own Key

AI features stay off until the user enables them. An AI action also requires a saved key for a configured provider. TrainIQ sends only the input needed for that action to the first provider in the user's configured order that has a saved key. This may include profile details (age, sex, height, weight, body-fat percentage, activity level, goal, and an optional manual calorie target) for goal advice; routine-generation details (goal, target focus, days per week, equipment, experience, duration, deload choice, priority muscles, preferred and excluded exercises, and existing exercise names); or, for a meal scan, the selected image, formatted capture time, suggested meal type, and sanitized user-provided scan context. A workout debrief may include training data. A user-requested weekly report may include training volume, weight trend, and the adherence percentage derived locally from completed-workout and meal timestamps. Saved foods, recipes, and meal-record notes are not included in current AI requests.

After a workout is completed, TrainIQ queues a debrief that may be processed later in the background when AI is enabled and a provider key is available. The debrief can include training volume, progression/comparison, muscle-group distribution, average RPE, top exercises, and weekly frequency. Other AI actions start from the related user action. The configured provider order may route a request to Google Gemini or OpenAI, depending on which keys are saved. Gemini requests carry the key in the `x-goog-api-key` header; OpenAI requests use an `Authorization: Bearer` header. Requests use HTTPS.

OWNER_CONFIRMATION_REQUIRED: confirm Google Gemini and OpenAI terms, regional availability, retention behavior, and whether production will move to a server-side gateway or OAuth-mediated access.

For a smart-scale/body-measurement reading, the selected image and any context the user supplies may also be sent to the configured AI provider.

## Telemetry and Diagnostics

Technical telemetry is off by default. If enabled and configured in a production build, TrainIQ is designed to upload privacy-safe technical events and performance summaries. Health data, notes, API keys, and meal photos must not be uploaded as telemetry.

OWNER_CONFIRMATION_REQUIRED: confirm telemetry endpoint, processor, retention period, and whether telemetry is enabled in production.

## Storage and Security

TrainIQ stores app data locally using Android storage mechanisms. Google Gemini and OpenAI API keys are stored with Android Keystore-backed encryption. Network calls use HTTPS; Gemini requests pass the API key in the `x-goog-api-key` header, and OpenAI requests use an `Authorization: Bearer` header, not a URL query parameter.

OWNER_CONFIRMATION_REQUIRED: confirm any additional server, backup, crash reporting, or analytics behavior before publication.

## Deletion and Control

Users can delete local app data from Settings. This clears local profile, training, nutrition, progress, preferences, AI keys, and Health Connect cache data on the device. Deleting a workout also requests cancellation of its queued background debrief. A request already sent to an external AI provider cannot be recalled by TrainIQ. Health Connect permissions are managed separately through Android Health Connect settings.

OWNER_CONFIRMATION_REQUIRED: confirm whether account deletion is applicable. The current local app scan found no account/auth system.

## Contact

OWNER_CONFIRMATION_REQUIRED: add privacy contact email, postal address if required, and response process.
