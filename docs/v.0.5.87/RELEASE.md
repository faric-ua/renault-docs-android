# Renault Docs v0.5.87 / build 103 — wrapped archive native root handoff

## User-reproduced issue #51

On v0.5.86, the user's **real** Megane II `NT8298A` ZIP failed with the native message "Вибрано батьківську або змішану папку", although their archive-manager screenshot shows a nested directory with `INDEX.HTM`, `COMMUN/`, and `RUS/`. The built-in source diagnostic confirmed **`ARCHIVE_FILE`**, `FAILED`, and no resulting package/sha256. No personally identifying SAF URI is copied to public documentation.

The actual selected extracted directory was not persisted in that diagnostic, so the exact original cause is **not proven** from the screenshot. This release adds last-mile automatic unwrapping plus actionable verification rather than guessing a deletion/repack.

## Code and safeguards

1. `ArchiveNativeRawRoot.resolve()` checks direct `INDEX.HTM` / `INDEX.HTML` / `ACCUEIL.HTM`. If the app-private extracted source is only a wrapper, it finds all child Renault raw roots and **only** redirects when exactly one exists. It refuses ambiguous multi-volume sources and missing entrypoints.
2. `NativePreparationStager.prepareLocal()` applies the resolver before its source walk and carries the resolved root through to `stageScan()`.
3. For explicitly selected multi-volume batch inputs with exclusions, never silently change the selected root; require an actual direct entrypoint. Nested selected-volume boundaries from v0.5.86 remain intact.
4. If scanning still produces no direct entrypoint, the error contains a bounded set of relative top-level file names and detected INDEX filenames, **never the SAF URI or full device filesystem path**.
5. Kotlin JUnit includes a synthetic ZIP with the observed `folder/folder/INDEX.HTM`, `COMMUN/`, `RUS/` shape; also refusal of ambiguous sibling volumes and missing INDEX. Python contracts cover wiring and privacy. Tests cannot prove behavior for the user's 72.7 MB ZIP until installed-device acceptance.

## Safety

Original ZIP/7Z/RAR, previously installed tomes and Classic are unchanged. No user data deletion, reimport or conversion is triggered during CI. Debug prerelease is not called production-stable. Issue #51 stays OPEN until the real NT8298A archive conversion succeeds or the enhanced diagnostic identifies the remaining mismatch. The single necessary user action after confirmed signed publication is updating over the current app and retrying this same ZIP once, then reporting result; no manual archive unpacking.
