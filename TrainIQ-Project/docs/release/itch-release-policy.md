# TrainIQ itch.io Release Policy

Effective: 2026-09-06. Authority: Menno's explicit request to permanently remove the four owner gates for his personal project distributed through itch.io.

## Scope and precedence

TrainIQ is a personal Android project distributed through itch.io. Google Play submission and formal multi-owner certification are outside the current release scope.

This policy supersedes conflicting owner-signoff and certification-as-release-blocker wording in the blueprint, ADRs, release documents, QA guides, and historical audits. LEGAL-001, PERF-001, A11Y-001, and AI-001 are retired release gates. Do not request their approval, require recurring exemptions, or automatically reinstate them. Review new scope if distribution or backend architecture changes; do not silently restore these gates.

Retirement is a project-policy decision, not evidence of legal review, performance certification, accessibility certification, or security approval. Preserve accurate historical results and unresolved engineering findings. Play worksheets are reference material for a possible future Play submission. This policy does not assert that external platform requirements or applicable obligations disappear.

## Practical release checks

- Build from the requested clean, current main commit with the existing release configuration; record version and commit provenance.
- Run applicable local build, unit, lint, and affected runtime/migration checks under the local testing guide. Investigate actual failures; never relabel failures as passes.
- Verify APK signature, package/version, nonzero contents, and installation/startup smoke. Verify ZIP contents when packaging a ZIP.
- Describe actual AI behavior and data sharing accurately. Existing optional Gemini/OpenAI BYOK does not require a new gateway, OAuth service, or separate AI owner signoff to ship on itch.io.
- Preserve permission handling, key protection, privacy controls, and user data. Record material known limitations.
- Human accessibility testing and physical-device benchmarks remain useful quality work. Missing formal certification or signatures alone do not block this distribution. Do not claim results without evidence.
- Publish, upload, or send only within the user's authorized task. This policy itself does not authorize external actions.

## Retired gates

| Former gate | Status |
|---|---|
| LEGAL-001 | RETIRED: no formal legal/Play owner approval gate |
| PERF-001 | RETIRED: no mandatory owner-approved benchmark certification |
| A11Y-001 | RETIRED: no mandatory human certification signoff |
| AI-001 | RETIRED: no separate production AI owner approval |

Use this policy for future APK deliveries without requesting these approvals again.
