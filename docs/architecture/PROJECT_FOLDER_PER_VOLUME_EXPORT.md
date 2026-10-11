# #162 — Stage 1: independent .rdpkg export to a user-selected folder

**Implementation candidate; Android CI and phone QA pending.** This is core functionality, NOT a login/payment/license change. Current phone v0.5.97/build113 remains unchanged until a new signed and approved build is actually installed.

## Home workflow (new)

Home → project `⋮`:

1. **Експортувати томи окремо (.rdpkg)** — new primary option. A preview shows total supported installed managed volumes, blockers and a warning that same-name files will never be overwritten. SAF `ACTION_OPEN_DOCUMENT_TREE` then asks the user to select the target folder (local storage, USB or a provider such as Google Drive).
2. **Увесь проєкт одним файлом (.rdproject) · офлайн** — existing optional one-file/offline transfer mechanism is fully retained: the existing `RdprojectShareService` and previously prepared file reuse are unchanged. The existing 1.26 GiB Megane II export on Drive is kept intact.
3. Existing project delete/prepared file management remains available.

The new `ProjectFolderExportService` uses `ProjectVolumeExportPlan` for an explicit, collision-safe ordered list and calls the existing validated `RdpkgExporter.export` **one tome at a time** directly into the selected SAF folder. It does NOT produce an outer `.rdproject` or re-convert Modern/Runtime/Fast Pack. It reads the output back through the SAF provider to verify actual SHA-256 against the bytes emitted by the exporter before renaming the application's own temporary `.part` to the canonical `.rdpkg` name.

If a same-name object already exists, skip it as `EXISTING_UNVERIFIED` with no overwrite or equality claim. On failure, delete *only this run's newly created temporary .part*, never delete, move or rename an installed volume, existing source ZIP, already-present remote package, or the previous 1.26 GiB .rdproject. No automatic package deduplication across distinct NT codes.

An accompanying small `RenaultDocs-<project>-<timestamp>-export.json` **inventory/report** is written into the selected folder on successful completion with project identity, distinct volume refs, new per-package names, digest and file counts, and explicit `EXISTING_UNVERIFIED` states. It sets `ready_for_public_catalog=false`. **This JSON is not `renault-docs-catalog.json`, has no provider-specific Drive file IDs and MUST NOT be automatically published.** Actual Drive metadata/ID and rights verification plus safe catalog update remain Stage 2.

Foreground service and its persisted `ProjectFolderExportRunStore` drive Home status while running and after COMPLETE/FAILED; cannot start again while the same service is running. Android 15+ dataSync timeout is handled as a FAILED interruption, not success. No repeat multi-volume job starts automatically after rotation.

## Risks / pending QA

- **NOT TESTED ON PHONE YET**: SAF providers, especially Google Drive, may append extensions or refuse rename of document files. On such failure, the process stops with an explicit error rather than publishing malformed filenames; no existing file is overwritten. Test against a *new empty private QA folder* before using populated catalogs.
- Interruption/process death, long exports (Android FGS quota #40), lock/rotation (#159), insufficient disk space, provider returning wrong bytes, partial exports and report creation must be verified on-phone. Previous completed volumes stay in target directory; next run will **skip as unverified** pending an explicit review. No generalized cross-process resumable upload or cancel UI in this first stage. Do not claim Stage 1 production-accepted until phone tests pass.
- `ProjectVolumeExportPlanTest` covers independent NT8340A/NT8342A vs name/ID collisions and unsafe output paths. Actual Android compile + Kotlin tests and exact signed artifact gates still needed.
- All installed tomes must support managed .rdpkg fast export; any SAF/legacy volumes are shown as blockers rather than silently dropped.
- The old .rdproject remains optional and untouched, still valuable for USB/offline whole-project transfer.

**Next Stage 2:** independently verify each remotely uploaded object by exact Drive ID + SHA256, avoid reupload when genuinely identical, safely generate/publish the small multi-project `renault-docs-catalog.json`, allow selective per-tome downloads, and implement robust resume/cancellation before Core Stable #147. Security/monetization #133 stays separate afterward.
