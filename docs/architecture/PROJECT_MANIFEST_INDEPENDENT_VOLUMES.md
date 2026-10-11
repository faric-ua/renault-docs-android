# Renault Docs — ADR: project = metadata + independent prepared tomes

**Status:** ACCEPTED AS TARGET ARCHITECTURE by owner on 2026-10-11; **Android project-share UX migration NOT IMPLEMENTED**, tracked at [#162](https://github.com/faric-ua/renault-docs-android/issues/162). Core release #147 remains OPEN; Google account / payments / entitlement controls #133 stay excluded until after Core Stable.

## Decision

- **Project** (`megane-ii`, `laguna-ii`, `kangoo-ii`) = durable logical group, model/title/applicability and references to individually identified installed/published volumes. A project **does not require** its content to be embedded in one multi-gigabyte ZIP.
- **Volume** = one portable self-contained `.rdpkg` with its own full Renault NT/variant/region/year identity, `package_id`, content, Modern/Runtime/Fast Pack and validation checksum. An individual `.rdpkg` may be distributed, downloaded, installed or checked without acquiring other volumes.
- **Cloud catalog** = a small `renault-docs-catalog.json` (already implemented, schema v1 / independently versioned `catalog_version`) that groups projects and records refs per volume. It contains `drive_file_id`, `file_name`, `size_bytes`, `sha256` and identity, **not archive data**. Drive file IDs identify the physical objects; matching filenames, timestamps or adjacent NT numbers never imply equal bytes/identity. Do not invent another mandatory manifest format if the existing catalog can represent the need.
- **Local project** = `ProjectStore` project metadata + associations to local `ProjectVolumeRecord` items (already implemented). Physical Drive folder layout is an organizational convenience; **neither source folder nor filename should determine ownership** over explicit `project_id` and the validated `package_id`.

### Current implementation audit

| Layer | Existing code | Assessment |
|---|---|---|
| One-volume package | `core/rdpkg.py`, `RdpkgExporter.kt`, `RdpkgImporter.kt` | **Already present** |
| Cloud metadata grouped into projects | `tools/build_drive_catalog.py`, `DriveCatalog.kt` | **Already present** |
| Select/download one or several individual volumes | `DriveCatalogActivity.kt`, `DriveCatalogClient.kt`, `CatalogImportService.kt` | **Already present**; evaluate integrity/download resumability separately |
| Phone project sharing | `RdprojectExporter.kt`, `RdprojectShareService.kt` | **Incorrect default for cloud sharing**: first re-ZIPs all expanded volumes individually into temporary .rdpkg, then wraps them in a large `.rdproject` ZIP |
| Package storage | `LocalDatasetDocumentsProvider.kt` | Current installed managed documents are **expanded** under app-private `noBackupFilesDir/rdpkg/<packageId>`. Per-volume export will still need one ZIP pass for missing remote copies; this ADR does not pretend compressed source packages remain in place |
| Historic `.rdproject` | `RdprojectExporter.kt` | Keep old file intact as an optional standalone/offline snapshot for compatibility; do not silently delete, downgrade or label its 21 inner volumes validated without audit |

A real 21-volume Megane II phone-generated `.rdproject` was uploaded to a **separate staging folder**, 1,262,821,306 bytes. It is still **PENDING internal integrity verification** because the current connector refuses whole-file downloads over 268,435,456 bytes. This does **not** block the new manifest-and-per-volume architecture and does **not** license deletion of the existing upload. The prepared catalog currently refers to 5 Megane II + 10 Laguna II volumes; it is not a declaration that the other 16 phone volumes or all original ZIPs are missing/invalid.

## Target cloud and phone workflow

<escape>Renault Docs cloud catalog (small JSON)
└─ Megane II (project_id: megane-ii)
   ├─ NT8340A → exact Drive file ID → one NT8340A .rdpkg
   ├─ NT8342A → exact Drive file ID → one NT8342A .rdpkg
   └─ … more separate tomes
└─ Laguna II (project_id: laguna-ii)
   ├─ one Laguna II .rdpkg
   └─ … more separate tomes</escape>

Physical packages can **eventually** be organized in `prepared/Megane II/`, `prepared/Laguna II/` or a flat folder, but **the current Python `build_drive_catalog.py` reads canonical packages from one flat `--packages-dir`**; support for nested source staging needs separate tooling changes. Remote folder layout is not authoritative for the Android downloader because it resolves an exact Drive file ID. A small catalog **does not contain** copies of the documents. An ID/metadata reference alone is not a proof of real file availability or checksum integrity.

1. **Inspect before writing.** Show project membership (e.g. 21 Megane tomes), already present/reusable validated remote objects, missing/needs-export tomes, duplicate-basename REVIEW, blocked tomes and estimated upload space. Do not choose a "newest" NT for the user.
2. **Per-volume packaging/export, only as needed.** A prepared managed volume currently lives expanded in private storage; use existing one-tome `RdpkgExporter` to stream that **one** volume into an independent `.rdpkg`. No temporary storage for 21 ZIPs together, no extra outer `.rdproject`. Potential future compressed/lazy-read on-device storage is a **separate** performance/migration decision.
3. **Independent upload/verify.** User-selected Google Drive destination, one independently retriable file per tome; record actual remote file ID, size and checksum. Existing remote packages may be reused **only after package identity and bytes/hash are independently verified**. No "same filename = same package" shortcut (#143). Never publish an unverified reference.
4. **Publish catalog last.** A small versioned catalog update is proposed only for the set of verified/approved `.rdpkg` refs, after user-approved destination and distribution rights; on failure retain the previous usable catalog. No automatic deletion/overwrites of existing Drive data.
5. **Consumer download.** User sees a project and independent list of tomes. Can select NT8340A only, several tomes, or **all** as a sequential/retryable per-tome queue; downloading a single tome must never fetch a giant multi-tome archive.
6. **Backup alternative.** The existing `.rdproject` is legacy/optional **offline single-file backup**, not the default cloud sharing path. Do not remove backward compatibility without dedicated migration tests.

## Metadata invariants and safety

- Source-of-truth identity uses `project_id` + `package_id` + full opaque NT document code, date/version/region/applicability. A `_1` or `_2` suffix in Renault source variant IDs has context-dependent meaning, never auto-deduplicate; NT8340A and NT8342A are **independent**.
- One Drive object selected by exact `drive_file_id`, its recorded file name/size/SHA-256 and content manifest. Different Drive files can share a basename. #143 remains review-only; no copies deleted.
- Don't auto-associate a Kangoo source/package to Megane because of a file picker path or similarly shaped archive. Reject explicit cross-project identity mismatches; flag unknown provenance for REVIEW.
- Preserve already installed 21 Megane tomes, 10 Laguna prepared catalog entries, original source ZIPs and historical snapshots. No automatic cleanup/overwrite, unapproved upload, backup points or protected license gating.
- Third-party Renault documentation redistribution rights need separate verification before public sharing; local/private audit is not publication approval.

## Release plan and acceptance

**This ADR is not an APK feature.** Keep #162 OPEN until separate release work implements default per-volume share/publish UI, stateful independent upload, selective download/restore and compatibility QA.

Proposed non-destructive tests:
- A synthetic 21 Megane + 10 Laguna manifest stays **small metadata-only JSON** with 31 distinct volume references, no embedded ZIP contents.
- Individual volume selection/download remains functional; project bulk selection schedules N separate `.rdpkg` jobs and no outer `.rdproject` unless explicitly chosen.
- Existing package reused only when exact ID/hash/content allow it; unresolved same-name versions are not silently chosen.
- Partial upload, failure, cancellation, rotation/lock/background restore progress without repeated mutations (#159), and old-catalog rollback.
- Existing `.rdproject` snapshot remains untouched and any advertised legacy import compatibility is actually tested (do not assume it is currently supported).
- Exact Android tests, signed APK and owner phone QA on an isolated new version; do not treat this design as passed implementation.

Relevant tracking: [#162](https://github.com/faric-ua/renault-docs-android/issues/162) for project-share redesign, [#30](https://github.com/faric-ua/renault-docs-android/issues/30) for package/catalog/source validation, [#147](https://github.com/faric-ua/renault-docs-android/issues/147) for Core Stable acceptance, [#133](https://github.com/faric-ua/renault-docs-android/issues/133) strictly post-Core Stable.
