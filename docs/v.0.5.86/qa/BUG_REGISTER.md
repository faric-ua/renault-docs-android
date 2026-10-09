# v0.5.86 issue #51 findings

**ARCH-ISOLATION-001 — CONFIRMED FROM CODE, PATCHED / PHONE PENDING**. Previous `NativePreparationStager.scanLocalSource()` traversed all directories under a chosen raw volume, so a root volume could include a nested separately selectable raw volume. The generated parent .rdpkg would contain unowned child files and possibly the wrong entrypoint context. `ArchiveRawVolumeIsolation` now excludes the candidate subtree at scan time. No deletion or staging mutation outside the normal temporary compilation process.

**ARCH-ROOT-MODEL-002 — SOURCE FIX**. Model/vehicle conflict validation in the initial extracted archive path and the resumed chooser now uses `ArchiveIntake.rawSourceName()` rather than the private `extracted` folder name. This does not replace full HTML metadata validation.

**OPEN:** Real mixed raw archives could have nested `INDEX.HTM` used as pages rather than truly independent volumes. Existing chooser heuristics classify these as candidates; verify on real archives rather than inferring success from filenames. Genuine ZIP/7Z/RAR import, encrypted/corrupt UX, cancellation/cleanup and lock/background acceptance remain open.

## ARCH-NT8298A-REAL-001 — confirmed real ZIP failure, root cause unproven (2026-10-09)

The existing read-only diagnostic confirms `ARCHIVE_FILE`, `FAILED`, Megane II project, source `NT8298A` ZIP with size 72,693,952 bytes, zero output package ID/SHA. User archive-manager screenshot shows valid-looking nested root with `INDEX.HTM`, `COMMUN/`, `RUS/`. Native `stageScan()` rejects for missing direct root index despite `ArchiveIntake.findRenaultRawRoots()` checking the same filename condition when identifying candidates. Suspected handoff/path mismatch **not proven** without actual ZIP contents or explicit internal staging-root evidence. Do not claim root-cause fixed. Do not ask for repeated conversion. Capture original ZIP offline for source-only reproduction if user permits, otherwise add scoped read-only diagnostic in future iteration.
