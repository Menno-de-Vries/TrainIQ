# TrainIQ Privacy Policy Draft

Last updated: 2026-05-12

Status: draft for owner/legal review. Every legal claim requires `OWNER_CONFIRMATION_REQUIRED` before publication.

Current release status: `BLOCKED`. Review this draft with `docs/release/owner-decision-packet-2026-05-10.md` (content refreshed on 2026-05-12), `docs/release/owner-action-tracker.md`, and `docs/release/play-console-owner-checklist.md`. Do not publish until production AI, telemetry, Health Connect, signing/versioning, and Data Safety decisions are final.

## Overview

TrainIQ helps users log training and nutrition and, when users choose, combine local app data with Health Connect data and Gemini AI features.

OWNER_CONFIRMATION_REQUIRED: confirm publisher legal name, contact email, jurisdiction, effective date, and policy URL.

## Data We Process

TrainIQ may store the following data locally on the device:

- Profile and goal data such as name/profile label, age, biological sex, height, weight, body-fat percentage, activity level, and goal.
- Training data such as routines, exercises, sets, workout sessions, workout notes, and active workout state.
- Nutrition data such as meals, foods, recipes, ingredients, macro estimates, and notes.
- Progress measurements such as body measurements and trend values.
- Health Connect data after permission is granted: steps, heart rate, sleep, active calories, weight, and exercise sessions.
- Health Connect cache metadata such as sync tokens and last sync timestamps.
- Gemini API key if the user chooses Bring Your Own Key AI.
- Technical diagnostics and performance summaries only if telemetry is enabled and configured.

OWNER_CONFIRMATION_REQUIRED: confirm final data categories against Play Console Data Safety wording.

## Health Connect

TrainIQ requests Health Connect access only after showing an explanation screen and the Android system permission flow. Users can grant or deny individual data types. TrainIQ should continue to work manually when Health Connect is unavailable or denied.

TrainIQ reads Health Connect data for activity, recovery, progress, and coaching context. Health Connect permissions can be changed or revoked in Android Health Connect settings.

OWNER_CONFIRMATION_REQUIRED: confirm exact Health Connect declaration and whether background reads are enabled in production.

## Gemini AI and Bring Your Own Key

AI features are optional. If enabled, TrainIQ uses the user's locally stored Gemini API key for explicit user-triggered actions such as meal analysis, routine generation, workout feedback, weekly reports, or goal advice.

For those actions, TrainIQ may send the prompt, relevant local context, and for meal scan flows the selected image to Google Gemini. TrainIQ does not run AI requests in the background.

OWNER_CONFIRMATION_REQUIRED: confirm Google Gemini terms, regional availability, retention behavior, and whether production will move to a server-side gateway or OAuth-mediated access.

## Telemetry and Diagnostics

Technical telemetry is off by default. If enabled and configured in a production build, TrainIQ is designed to upload privacy-safe technical events and performance summaries. Health data, notes, API keys, and meal photos must not be uploaded as telemetry.

OWNER_CONFIRMATION_REQUIRED: confirm telemetry endpoint, processor, retention period, and whether telemetry is enabled in production.

## Storage and Security

TrainIQ stores app data locally using Android storage mechanisms. Gemini API keys are stored with Android Keystore-backed encryption. Network calls to Gemini use HTTPS and pass the API key in the `x-goog-api-key` header, not in the URL.

OWNER_CONFIRMATION_REQUIRED: confirm any additional server, backup, crash reporting, or analytics behavior before publication.

## Deletion and Control

Users can delete local app data from Settings. This clears local profile, training, nutrition, progress, preferences, AI key, and Health Connect cache data on the device. Health Connect permissions are managed separately through Android Health Connect settings.

OWNER_CONFIRMATION_REQUIRED: confirm whether account deletion is applicable. The current local app scan found no account/auth system.

## Contact

OWNER_CONFIRMATION_REQUIRED: add privacy contact email, postal address if required, and response process.
