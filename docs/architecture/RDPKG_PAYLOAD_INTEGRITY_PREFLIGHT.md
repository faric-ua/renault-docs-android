# Issue #30 — Stage F: full read-only `.rdpkg` payload integrity check

**Scope:** prepared package quality in the **core Renault Docs** release. No account permissions, encryption, payment, Google Play or access gating (#133 remains blocked until core stable #147).

## Why metadata preflight alone is insufficient

An existing catalog builder inspects `rdpkg.json`, canonical filename, package ID and locally calculated ZIP SHA-256. Stage E compares selected Drive file ID/name/size and optional provider checksum. A file can still be a malformed or inconsistent `.rdpkg` with readable top-level metadata but damaged payload. Stage F explicitly verifies the **local package's contents**, not just its outer ZIP hash.

## Validation contract

`tools/validate_rdpkg_payload.py:validate_rdpkg_payload(path)` performs **read-only, streaming** verification:

- ZIP can be opened; metadata `rdpkg.json` has supported format/schema and is bounded to 2 MiB; the embedded dataset JSON is bounded to 16 MiB.
- No duplicate ZIP names (including case-insensitive collisions), absolute paths, `..` traversal, Windows drive paths or backslash separators; no symlink or encrypted entries. No extraction or file writes.
- `rdpkg.json` declares a valid single volume and names a real file entrypoint and dataset manifest.
- Embedded dataset and package agree on `project_id`, applicable full volume ID/document code/date and entrypoint (when present). **NT8340A and NT8342A remain separate documentation**; no supersession or automatic deduplication.
- Excluding the `rdpkg.json` metadata entry, actual ZIP payload file count and uncompressed byte sum must equal `payload_file_count` and `payload_bytes`. A 32-GiB uncompressed guardrail applies by default.
- Every payload is fully decompressed to a temporary **in-memory streaming buffer only** and its CRC is checked by Python ZipFile; no decompressed files are stored.
- On failure, raise a specific internal `PackageIntegrityError`; the publication preflight maps it to safe public reason `local_package_payload_integrity_failed` without revealing local file paths/provider IDs.

`tools/preflight_drive_publish_plan.py` now calls this validator **before** checking remote metadata. Invalid local ZIP payload leads to `BLOCKED` even if a Drive ID/name/size would match. Metadata or CRC success does not prove original Windows ZIP-to-output parity, PDF correctness, Drive remote byte equality, legal rights, or permission to publish.

The existing `tools/build_drive_catalog.py` catalog builder remains unchanged in this iteration. Call Stage E/Stage F preflight **before** any manual catalog-generation/publishing workflow; there is still no automatic publisher. The stored user's files, Drive objects, and installed app v0.5.97 are unchanged.

## Regression

- Realistic synthetic `.rdpkg` fixtures and a positive no-write streaming test.
- Reject missing entrypoint, wrong payload-file counters/bytes, conflicting dataset model or document identity, unsafe paths, symlink, duplicate names, invalid ZIP and CRC-corrupted payload.
- Existing Stage E exact-ID/hash/size/collision tests continue passing; integration test makes sure unsafe local payload is `BLOCKED` despite matching metadata.
- Require exact PR-head Python CI PASS and main CI PASS before merge/marking Stage F complete.

**Release boundaries:** core-stable QA #147 still OPEN; access/security #133 BLOCKED. Actual on-phone import and remote Drive bytes/hash parity are separate required acceptance gates.
