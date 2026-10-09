# v0.5.86 issue #51 findings

**ARCH-ISOLATION-001 — CONFIRMED FROM CODE, PATCHED / PHONE PENDING**. Previous `NativePreparationStager.scanLocalSource()` traversed all directories under a chosen raw volume, so a root volume could include a nested separately selectable raw volume. The generated parent .rdpkg would contain unowned child files and possibly the wrong entrypoint context. `ArchiveRawVolumeIsolation` now excludes the candidate subtree at scan time. No deletion or staging mutation outside the normal temporary compilation process.

**ARCH-ROOT-MODEL-002 — SOURCE FIX**. Model/vehicle conflict validation in the initial extracted archive path and the resumed chooser now uses `ArchiveIntake.rawSourceName()` rather than the private `extracted` folder name. This does not replace full HTML metadata validation.

**OPEN:** Real mixed raw archives could have nested `INDEX.HTM` used as pages rather than truly independent volumes. Existing chooser heuristics classify these as candidates; verify on real archives rather than inferring success from filenames. Genuine ZIP/7Z/RAR import, encrypted/corrupt UX, cancellation/cleanup and lock/background acceptance remain open.
