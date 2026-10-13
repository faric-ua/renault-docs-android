# #162 — Stage 1: independent .rdpkg export to a user-selected folder

**Stage 1 was merged and CI-accepted in v0.5.98/build114; phone verified 21/21 managed Megane II tomes preflight. Stage 1b (v0.5.99/build115) adds one-vs-all selection; its CI and phone QA remain pending until explicitly passed.** This is core functionality, NOT a login/payment/license change. Install a new stable-signed build over the prior app, never clear installed data.

## Home workflow (new)

Home → project `⋮`:

1. **Експортувати томи окремо (.rdpkg)** — primary option. A preview shows total supported installed managed volumes, blockers and the no-overwrite warning. Choose **Один том** to open a scrollable list of distinct volume IDs / full NT identities and select exactly one, or **Усі томи** to explicitly queue all available volumes. Only then SAF `ACTION_OPEN_DOCUMENT_TREE` asks for the target folder (local storage, USB or Google Drive). No selection defaults to all; cancel means no export.
2. **Увесь проєкт одним файлом (.rdproject) · офлайн** — existing optional one-file/offline transfer mechanism is fully retained: the existing `RdprojectShareService` and previously prepared file reuse are unchanged. The existing 1.26 GiB Megane II export on Drive is kept intact.
3. Existing project delete/prepared file management remains available.

The new `ProjectFolderExportService` uses `ProjectVolumeExportPlan` for an explicit, collision-safe ordered list and calls the existing validated `RdpkgExporter.export` **one tome at a time** directly into the selected SAF folder. It does NOT produce an outer `.rdproject` or re-convert Modern/Runtime/Fast Pack. It reads the output back through the SAF provider to verify actual SHA-256 against the bytes emitted by the exporter before renaming the application's own temporary `.part` to the canonical `.rdpkg` name.

If a same-name object already exists, skip it as `EXISTING_UNVERIFIED` with no overwrite or equality claim. On failure, delete *only this run's newly created temporary .part*, never delete, move or rename an installed volume, existing source ZIP, already-present remote package, or the previous 1.26 GiB .rdproject. No automatic package deduplication across distinct NT codes.

An accompanying small `RenaultDocs-<project>-<timestamp>-export.json` **inventory/report** is written into the selected folder on successful completion with project identity, distinct volume refs, new per-package names, digest and file counts, and explicit `EXISTING_UNVERIFIED` states. It sets `ready_for_public_catalog=false`. **This JSON is not `renault-docs-catalog.json`, has no provider-specific Drive file IDs and MUST NOT be automatically published.** Actual Drive metadata/ID and rights verification plus safe catalog update remain Stage 2.

The selected ordered opaque volume IDs persist through the SAF picker across Activity recreation/rotation, and are revalidated by the foreground service before any file write. The inventory's expected volume count equals the **selected subset**, not necessarily all installed project volumes. Selection is explicitly persisted in `ProjectFolderExportRunStore`; no missing/unknown/duplicate ID can silently expand to all volumes. Foreground service and its persisted `ProjectFolderExportRunStore` drive Home status while running and after COMPLETE/FAILED; cannot start again while the same service is running. Android 15+ dataSync timeout is handled as a FAILED interruption, not success. No repeat multi-volume job starts automatically after rotation.

## Risks / pending QA

- **SINGLE-VOLUME SAF PHONE TEST PENDING:** first install the exact stable-signed v0.5.99 build over existing data and confirm 21 Megane II tomes remain. Choose **Один том** and pick one full NT identifier. Then select a *new empty private QA folder* in Google Drive to test one exact .rdpkg + one small JSON inventory; do not queue all 21 until this passes. Providers may append extensions or refuse rename; on such failure, stop with an explicit error, do not overwrite existing files.
- Interruption/process death, long exports (Android FGS quota #40), lock/rotation (#159), insufficient disk space, provider returning wrong bytes, partial exports and report creation must be verified on-phone. Previous completed volumes stay in target directory; next run will **skip as unverified** pending an explicit review. No generalized cross-process resumable upload or cancel UI in this first stage. Do not claim Stage 1 production-accepted until phone tests pass.
- `ProjectVolumeExportPlanTest` covers independent NT8340A/NT8342A vs name/ID collisions and unsafe output paths. Actual Android compile + Kotlin tests and exact signed artifact gates still needed.
- All installed tomes must support managed .rdpkg fast export; any SAF/legacy volumes are shown as blockers rather than silently dropped.
- The old .rdproject remains optional and untouched, still valuable for USB/offline whole-project transfer.

**Next Stage 2:** independently verify each remotely uploaded object by exact Drive ID + SHA256, avoid reupload when genuinely identical, safely generate/publish the small multi-project `renault-docs-catalog.json`, allow selective per-tome downloads, and implement robust resume/cancellation before Core Stable #147. Security/monetization #133 stays separate afterward.

## Stage 1c — arbitrary subset checkbox export (v0.5.100/build116; phone QA pending)

The owner verified **22 tomes** on v0.5.99 after adding a new volume to the previous 21-volume Megane II dataset, and requested arbitrary subsets rather than only one-or-all. The SAF preflight now offers **Вибрати томи** (multi-choice checkbox list with live `Вибрано X із Y`, **Вибрати всі / Очистити**, and **Вибрати папку** disabled for empty selection) or explicit **Усі томи**. Selecting checkboxes never starts export. The selected **opaque volume IDs** are saved in the dialog state across rotation and passed unchanged through the SAF chooser; both MainActivity and ProjectFolderExportService independently validate exact IDs against the project plan. Only selected packages are emitted, in project order; JSON expected volume count is for the selected subset. Back/cancel = no new export. Existing no-overwrite, SHA readback, .part cleanup and Drive catalog isolation remain unchanged.

**Separate unresolved safety issue #173:** before/after v0.5.99, the owner reported previously prepared 21-volume `.rdproject` disappeared from menu after adding a 22nd tome. Code has a likely destructive path `upsertVolume → invalidatePreparedProject → deleteProject`; do not assert device file deletion without read-only check. **Do not repack, silently delete, rename or overwrite historical .rdproject.** Verified external Drive archive copy remains. This Stage 1c only changes the independent `.rdpkg` export selection, NOT the legacy `.rdproject` lifecycle. Fix #173 separately with explicit historical snapshot retention and QA.

**Phone acceptance pending:** install the exact stable-signed build over existing without clear/uninstall, check 22 tomes preserved, select any 3 non-contiguous NTs in the multi-choice list, rotate and verify the same 3 checks, cancel/back no write, then export to a NEW EMPTY PRIVATE SAF test folder. Verify 3 distinct `.rdpkg` files and one small JSON report with `expected_volumes=3`; no 19 unselected packages, no published Drive catalog. Never test in the populated Drive publication folder.


## Stage 2 (v0.5.123, 2026-10-13) — catalog from the phone export report

**Owner decision 2026-10-13 15:39 / 19:27:** publish all 22 Megane II volumes. «На Google диску кожен том окремо; людина тягне один або два, а не всі 22. Хто хоче всі офлайн — пакує і передає з телефона на телефон» (that is the optional `.rdproject`).

Flow:
1. Phone (v0.5.123+): Home → Megane II ⋮ → «Експортувати томи окремо» → «Вибрати томи» → the volumes not yet in the catalog → Drive folder `Renault Docs Projects / Megane II — нові томи 2026-10-13` (created empty by Claude, inside the public folder, so files inherit link access). The app reads every copy back through the Drive provider (SHA-256) and writes the report JSON, now **schema 2** with `package_id` (from the package's own `rdpkg.json`), `vehicle_codes`, `document_type`, `document_version`, `region`.
2. Claude: Drive folder listing (exact ids, names, sizes) + the report → `tools/catalog_from_phone_export.py`. A volume is added only if the phone verified it, exactly one Drive file has that name and the same size, and its package id / name do not clash with a different published volume. Same name twice → `REVIEW_REQUIRED`; anything else wrong → `BLOCKED`; then no output at all. Nothing is removed from the catalog; the current catalog file is never modified — a new versioned file is written.
3. Owner says «так» → Claude uploads the new catalog as a **new** Drive file and releases an app version pointing to it (the connector cannot replace the content of an existing Drive file; the old catalog stays as history).
4. Download side (v0.5.123): `DriveCatalogClient.downloadPackage` now checks size and the catalog `sha256` of every downloaded `.rdpkg` before installing; on mismatch only the app's own temporary download is removed and the volume is not installed.

Limits: the connector shows no Drive checksum, so the Drive side is checked by exact id + name + size; the content check is the phone's read-back SHA-256 at export time and the app's SHA-256 check at every download.
