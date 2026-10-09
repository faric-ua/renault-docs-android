# v0.5.86 phone QA — DEFERRED at user's request

No device test and no app installation requested automatically.

Future safe test: create/choose controlled mixed-root archive containing a root raw volume (INDEX.HTM), a nested independent NT volume (INDEX.HTM), and a normal shared asset. Confirm chooser lists both; creating root produces only root's assets/entries and no nested independent volume; creating the nested one produces exactly its own files. Confirm package identity, duplicate prevention, source archive preservation, temporary staging cleanup, rotation/lock, and explicit cancellation behavior. Repeat with other supported formats when sample archives are available. Observe file results; do not mark PASS based on compilation alone.

#40 Android background quota/timeout is still separately open. No force-stop/reboot guarantees.


## Release readiness — 2026-10-09

Stable signed debug APK #149 PASS and public verified prerelease https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.86-debug are available. **No v0.5.86 installation or on-device archive import took place.** Previous v0.5.84 was phone-accepted only for over-install/data retention; do not ascribe this result to v0.5.86. Extended phone QA remains paused, issue #51 OPEN.

## Phone Test 1 — installation/data retention: USER PASS (2026-10-09)

User replied «Пасс» directly to the requested 4-step live check: Renault Termux `5 → 19 → 8 → 13`, install v0.5.86/build102 over the existing app without uninstall/clear data, launch Renault Docs, and verify registered automobiles and volumes are still present. **PHONE PASS for installation + app launch + retention only**. Per-volume counts were not independently reported. ZIP/7Z/RAR actual intake, parent/child .rdpkg package content, cancel/lock/background and source staging are NOT verified yet.

## Next live test: archive source confirmation (PENDING)

Open a project matching an existing Renault ZIP/7Z/RAR → `Додати` → `Створити .rdpkg з архіву` → choose the archive with SAF. Expect the dialog `Підтвердь джерело архіву` showing the correct archive filename/project and duplicate-warning text. **Do not press Continue/choose destination yet**. Send screenshot; no native conversion should have started. This is an input/preflight test, not a successful ZIP/7Z/RAR import claim.

## Phone Test 2 — archive source preflight screenshot PASS (2026-10-09)

User supplied a screenshot of the app's `Підтвердь джерело архіву` dialog in **Megane II**, showing source `Megane II B,C,S 84_NT8298A_Visu v3.0_2005.11.28.zip` and project `Megane II`, message `У каталозі встановлених томів збігів за NT немає.`, technical details collapsed and `Скасувати`/`Продовжити` buttons. **PHONE PASS for project/source identity and read-only catalog-NT preflight UI only**. The message does not mean the archive payload was inspected or that full content duplicates were excluded; no conversion or APK package result has yet been evidenced. Next safe action is to continue and select a separate output folder intentionally to perform a real archive-to-RDPKG test, then capture result. Do not delete original archive or existing volumes.

## Phone Test 3 — actual NT8298A ZIP → .rdpkg FAILED (2026-10-09)

User proceeded from the previously accepted ZIP source preflight and selected an output SAF folder. On Megane II ProjectActivity the final native status (screenshot) displays **`Не вдалося створити .rdpkg: Вибрано батьківську або змішану папку. Для .rdpkg вибери raw-папку одного Renault тому, де INDEX.HTM / INDEX.HTML / ACCUEIL.HTM лежить у корені.`**. Project header still shows **`Megane II · томів: 10`**. Source ZIP selected on earlier preflight: `Megane II B,C,S 84_NT8298A_Visu v3.0_2005.11.28.zip`.

**PHONE TEST 3 FAIL**, NOT PASS; root cause still undetermined. Source contract confirms the message comes from `NativePreparationStager.stageScan()` when `hasRootEntrypoint(scan.files)` is false. That condition indicates the scanned file set does not include a directly-rooted INDEX.HTM / INDEX.HTML / ACCUEIL.HTM, but does *not* alone identify whether incorrect chooser rawRoot, missing extracted entrypoint, native scan path exclusion, or unintended RAW_TREE source kind caused it. Next **one non-destructive diagnostic action**: in the same project, tap `Джерело останньої .rdpkg · діагностика` and show phase, source kind and name (redact physical URI/Document ID). No repeat conversion, no new APK or cleanup before evidence. Issue #51 remains OPEN and now has direct failing device evidence.

## Phone Test 3 — archive-manager structure screenshot (2026-10-09)

Follow-up screenshot from user's ZIP archive manager confirms the original selected Renault source is nested under at least one containing folder, and the displayed innermost directory contains `INDEX.HTM` (653 bytes), `COMMUN/` and `RUS/`. This is an apparently valid candidate raw root **inside the source ZIP**, not an arbitrary parent directory. Source screenshot does **not** expose complete ZIP entry listing or which canonical extracted raw directory the service selected.

Previous app failure is from `NativePreparationStager.stageScan()` when its scanned relative file set lacks a directly-rooted `INDEX.HTM`, `INDEX.HTML`, or `ACCUEIL.HTM`. Given this new evidence, inspect **the handoff between `ArchiveIntake.findRenaultRawRoots()`, archive candidate selection / `processPreparedSource()`, and `NativePreparationStager.scanLocalSource()`**; do not instruct user to manually unpack/restructure the archive. Need the existing read-only diagnostic `Джерело останньої .rdpkg · діагностика` to confirm sourceKind and sourceName; if needed, future in-app safe internal diagnostic for extracted selectedRoot/relative files. Root cause not yet proven. **No repeat conversion, source mutation, or new release required at this point.**
