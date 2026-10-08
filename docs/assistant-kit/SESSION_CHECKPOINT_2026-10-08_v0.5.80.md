# Renault Docs — v0.5.80 archive-source safety checkpoint — 2026-10-08

## Release
**v0.5.80 / build 96 — MAIN CI PASS / PHONE QA PENDING (NOT CLOSED).**
Repo `faric-ua/renault-docs-android`. App source commit: `5a6ddc060edb4ff452eb1e3e96c1423cc042ddf9`.
PR #86 merged; feature PR Tests #562 PASS / Android PR Check #449 PASS.
Main Tests #563 PASS / Android Debug APK #143 PASS.
Signed stable Debug artifact: `Renault-Docs-v0.5.80-Debug`, id `11557433214`.
SHA256 artifact: `0da82672e308148e5a2178f9fb147705b5ab8f2b96c39da15220e09dde6223b6`.
Artifact expires 2026-10-11. Usual installation: Renault Menu `5 → 19 → 8 → 13`, install over existing app without uninstall/clear.

## User decision and implementation

NT8486 incident: v0.5.79 source diagnostic proved actual saved SOURCE = `Kangoo-II_X61_NT8486_Visu-v5.0_2009-08-31.rdpkg`, provider `com.android.externalstorage.documents`, Document ID `primary:Documents/Renault/packages/rdpkg/Kangoo-II_X61_NT8486_Visu-v5.0_2009-08-31.rdpkg` (94,965,991 bytes). TARGET = `megane-ii`. Result = `megane-ii-nt8486-2009-08-31`, SHA256 `4ed38ab4a3ea8dcf41c24cf86d25820d820f896652062b682eab7f1ef0de53f2`. 2026-10-08 15:13:32 → 15:15:50 phone local.
User acknowledges accidentally selecting nearby existing package is plausible. User explicitly requests:
1. Prevent re-converting existing `.rdpkg` as raw archive.
2. Block known Kangoo/Megane/Laguna model conflicts; don't insert wrongly labeled content.
3. Search for duplicates across ALL registered projects and show where similar documents already live.
4. Preserve source ZIP, generated .rdpkg, installed tomes. No automatic deletion/move; user moves ZIP Sources→Backup manually.

In v0.5.80:
- `ArchiveSourceGuard` checks source extensions `.zip/.7z/.rar`; rejects `.rdpkg` at UI and service; removes broad octet-stream source picker MIME filter. If selected ZIP contains a prepared `renault-dataset.json`, `ArchiveIntake.inspectRawRoots()` flags it before extraction.
- Explicit source/root names Kangoo, Megane, Laguna checked against chosen project before writing. DO NOT infer from alone NT or opaque Renault platform code X61.
- `ProjectActivity.showArchiveSourcePreflight()` presents exact source display name, SAF Document ID and authority, destination project, plus `ProjectStore.projects()` × volumes matches on NT. This is metadata similarity, **not** SHA/binary identity, and it reads only registered application catalog (not arbitrary filesystem/Drive). User must explicitly Continue before destination choice; Cancel/rotation does not start a job.
- `startNativeArchiveRdpkgFlow()` no longer clears completed diagnostic record just on opening archive picker. Runtime source/new outputs are not touched by release migration.
- Kotlin JUnit `ArchiveSourceGuardTest.kt` and Python `test_v0580_archive_source_guard_contract.py`, CI PASS.

## Known limitations / separate issues

- Models with unfamiliar names or without reliable source labels may need stronger internal checks in later release; no every-PDF deep-scan or universal vehicle inference is claimed.
- Global duplicate check only in `ProjectStore` registered volumes. Matching NT across vehicles is a **possible** duplicate and never causes auto deletion/relocation.
- User's historical Android picker navigation is not recorded: issue #83 remains an unsolved UX provenance question, but actual persisted source is known.
- #85 duplicate .rdpkg guard and #82 model mismatch now implemented as **candidate / phone QA pending**; do not close before phone acceptance.
- #81 source immutability; original Kangoo ZIP located both on device search and connected Drive Kangoo folder. No proven deletion by app.
- #79 Home → New volume bypass source type selection still OPEN / deferred.
- v0.5.78 volume Add/status progress phone QA not fully signed off; Home-page collapse/pin/status adoption remains separate after stable source safeguards.
- Existing wrong-model `Megane-II_X61_NT8486` and all original files remain untouched until explicit user decision after visual QA.

## Exact next step

Install build96 **over current installation** with stable signer; **do not delete data, uninstall or clear cache**.
Test using read-only selections first:
- Source picker either hides prepared `.rdpkg` or rejects it if provider exposes it; no new output started.
- Wrong Kangoo original ZIP selected for Megane blocks with explicit conflict **before** destination.
- Genuine Megane ZIP shows preflight exact Document ID/name, target and all project NT similarities; Cancel does not start.
- Rotate while preview visible; expect preview restored and no automatic run.
- Cross-project catalog matches can be marked PASS only when relevant NT exists in registered volumes; avoid claiming byte-level identity.
- Use disposable archive or safe Cancel-only preflight for initial QA; do not delete/move/overwrite originals or generate duplicate output solely for tests.

Keep phone report as PASS / FAIL / SKIP checklist; no release closeout before user evidence.
