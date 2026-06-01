# TrainIQ Repo Cleanup Audit

Updated: 2026-06-01

## Removed from tracked source

The following generated/local-output areas were removed from git tracking and are now covered by `.gitignore`:

- `TrainIQ-Project/.codex/`
- `TrainIQ-Project/.kotlin/errors/`
- `TrainIQ-Project/dist/`

These paths contain local automation state, compiler error logs, release bundles, and upload-pack artifacts. They should be regenerated locally when needed instead of versioned as source.

## Intentionally retained

`TrainIQ-Project/docs/qa/` still appears in `git ls-files -c -i --exclude-standard` because the root `.gitignore` has a broad docs/QA ignore rule while historical QA plans and evidence are already tracked.

Do not delete these files as routine cleanup. Treat them as retained product/QA history unless a separate archival task explicitly moves or removes them.
