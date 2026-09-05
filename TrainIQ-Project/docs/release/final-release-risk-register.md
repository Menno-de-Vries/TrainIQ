# Release Risk Notes

Updated: 2026-09-06.

The [itch.io release policy](itch-release-policy.md) governs delivery. LEGAL-001, PERF-001, A11Y-001, and AI-001 are retired and do not block releases. Missing owner signatures do not impose a blanket BLOCKED status.

## Evidence and limitations

- Performance: historical SM-S931B measurements exist. They do not establish universal performance certification; report actual measured coverage.
- Accessibility: automated semantics and runtime checks exist. Describe human TalkBack/Switch Access coverage accurately; missing certification is not an itch.io release veto.
- AI: preserve implemented optional Gemini/OpenAI BYOK behavior and protections. No new gateway or owner approval is required by this policy.
- Health Connect and scanner: verify affected flows on safe test setups and disclose untested provider, permission, camera, or device cases. Assess actual defects under the normal engineering workflow.
- Signing/versioning: the 2026-09-05 release from main 31ab4b8 passed signature verification, installation/startup smoke, and ZIP validation. Verify future artifacts individually.
- Privacy and distribution: describe actual data handling. Play submission worksheets are outside the current distribution scope.

Record build-specific PASS, FAIL, and NOT RUN results with provenance. This policy change closes no technical finding and fabricates no certification evidence.
