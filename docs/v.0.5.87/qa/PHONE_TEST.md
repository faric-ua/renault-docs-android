# v0.5.87 phone acceptance — SINGLE ACTION AFTER VERIFIED RELEASE

**Do not ask for testing before signed APK is published.** Update Renault Docs in-place; do NOT uninstall, clear project data, move or unpack original archives.

Only one live check requested after install: Megane II → Add → Create .rdpkg from archive → pick the same NT8298A ZIP → confirm source → choose the intended export directory. Report the final Completed/Failed state and whether volume count changes (was 10 before test). If failed, send exact new bounded error including `файли верхнього рівня` and `знайдені INDEX`; no full SAF URI necessary. If succeeded, verify resulting package label and exactly one new volume; original file remains intact.

Success of synthetic ZIP tests and installation smoke must not be confused with this real-archive acceptance. Do not invent a PASS.

## Signed APK readiness — 2026-10-09

All code/CI delivery gates PASS: PR Python #591, Android PR #467, main Tests #592, stable signer APK #150 (run 37938581867), explicit immutable promotion #99 and verified release publisher run 37938990631. GitHub release: https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.87-debug; actual APK SHA256 `60795a057ad77b96df0f914a8c9341222d9684fc0087e89d9389c558761286f5`. **Superseded by the real NT8298A PHONE PASS recorded below.** Existing source archives must remain unchanged.


## NT8298A real archive end-to-end — PHONE PASS (2026-10-09)

The user performed the same real ZIP → native .rdpkg flow on the signed **v0.5.87/build103** candidate. Native in-app terminal status and subsequent built-in *read-only* operation report are consistent:

- **Phase:** `COMPLETE`, **sourceKind:** `ARCHIVE_FILE`, source display name `Megane II B,C,S 84_NT8298A_Visu v3.0_2005.11.28.zip`, source size **72,693,952 bytes**.
- **Project:** `Megane II (megane-ii)`; resulting volume label `NT8298A · 2005-11-28`.
- **Package ID:** `megane-ii-nt8298a-2005-11-28`.
- **Native sections:** **319** (this is `sectionCount`, **not** the number of files).
- **Generated package SHA-256:** `e7fdbea2d3363af3ea3710eda22dcee518e36d08963b3483f0610a55602f6603`.
- **Start:** 2026-10-09 16:57:20; **finish:** 2026-10-09 16:59:19 (**1m59s**, as displayed by user device).
- **Destination:** SAF folder selected for exported RDPKG; no full source/output URI or physical `Documents` path is copied into the public QA record.

**PASS scope:** the previously failing real **nested raw root** NT8298A ZIP was prepared, validated and completed successfully, with a stable package ID and digest. `NativeRdpkgPreparationService.processPreparedSource()` calls `RdpkgImporter.install()` and `ProjectStore.upsertVolume()` before setting `COMPLETE`, so the completion path includes registering the imported volume. The user did **not** separately report the post-operation displayed volume count, open/test each native section, compare extracted content byte-for-byte, or check source archive unchanged by independent filesystem hashing; do not mark those as separately accepted. Old v0.5.86 failure is resolved for this concrete archive on v0.5.87.

### New phone UX finding (TRACKED / NO IMPLEMENTATION)

During the successful archive preparation, user observed unstable **live status detail text**: `Копіюю файли` / `Готую` / `Пакую` with file-count lines alternating with bare counts or missing counts, causing the detail row to jump vertically/horizontally. This is not a conversion failure. User explicitly asked to **record only**, make **no changes** yet. See GitHub issue **#100** (new UX backlog). Possible causes in `OperationProgress.displayText()`, alternating service `onProgress` / `onProgressState` messages and `OperationStatusView` adaptive 2-line portrait/1-line landscape are **hypotheses**, not proven without video/repro.

### User requested read-only deletion and sharing risk assessment

User asked whether removing a volume/file/data for Megane II vs Kangoo II could remove one or both similar named files. Do not perform a delete test, rename source, clear app data, or delete RDPKG. Code audit in `docs/v.0.5.87/qa/DELETE_BEHAVIOR_READONLY.md` distinguishes remove-project-link vs delete prepared share copy vs Android app clear-data, and checks per-project package identity.
