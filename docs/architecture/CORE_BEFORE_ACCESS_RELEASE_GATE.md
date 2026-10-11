# Renault Docs — two-stage release contract (owner-approved 2026-10-11)

## Rule: Core first; access / payments / protected distribution later

**Gate A — finish and accept independent Renault Docs core.**

All core functionality that will ship in the baseline must work **without a Google account, invitation, subscription, purchase, entitlement server or encryption unlock**. No new access restrictions or license checks are introduced before the core release.

1. Agree final core scope; complete functionality, UX, data integrity, conversion/import/export/Drive catalog and applicable defects. Mark each issue explicitly done, postponed with rationale, or release-blocking.
2. Run tests **against the exact final source SHA** (Python + Android/Kotlin + required package/parity tests); do not claim PASS for an unrun emulator scenario (Android 15+ dataSync quota remains separately pending #40).
3. Build signed release by the established stable signer. Pin immutable source SHA, run ID, signer, APK SHA-256, published package identity and tested upgrade path.
4. Phone QA on the exact signed build: install over existing data; check volumes and original documents, native and legacy routes, PDF, rotation, original-archive flow, background/lock & cancellation, catalog behavior. Negative cases only with safe non-destructive test fixtures. Restore/cancel/failed status must not remove committed volumes. Document results and limitations.
5. Only after evidence and explicit owner acceptance: merge all required core work, tag/publish a standalone **CORE STABLE** release with version, release notes, checksums and a handoff checkpoint. Label **CORE STABLE** only when actually verified. Do not infer that the present v0.5.97/build113 is this final milestone.

**Gate B — separate access/security monetization release.**

Issue #133 remains **TODO / blocked** until Gate A is satisfied. Afterward use a new branch, separate build, distinct release notes/QA and explicit migration plan for existing installed tomes. Scope includes Google identity with verified subject, owner-managed friend grant list, one-time Google Play product and optional subscriptions if separately approved, backend purchase verification, tamper signals, encrypted content distribution, offline entitlement, account restoration and refunds. Do not embed e-mail list or secrets in APK. Verify third-party Renault documentation redistribution rights before sales/distribution.

Never silently lock or migrate existing content or require sign-in in the core baseline. The two releases must be independently installable/testable and have separate phone-acceptance records. Safety and data-integrity fixes in core are still permitted; **authentication, billing, feature-locking and cryptographic content entitlement are not**.

## Current status

- Current user phone: v0.5.97/build113, core development ongoing.
- #30 source/archive/catalog pipeline OPEN; #143 same-name prepared-package copies under review without deletion.
- #133 postponed to **after** complete core release. No actual security/access implementation is approved for the core release.
