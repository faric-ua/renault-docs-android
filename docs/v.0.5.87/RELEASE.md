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

## Verified build and public signed debug prerelease — 2026-10-09

- PR #98 merged Android app source SHA `937ffa06080c3dd6a63a87d4e9c5d209ff4b8755`; Python PR Tests #591 PASS, Android PR Check #467 PASS, main Tests #592 PASS, developer-signed Android Debug APK #150 PASS, run ID `37938581867`.
- Original signed source artifact `Renault-Docs-v0.5.87-Debug`, ID `11620685925`, Actions **bundle** digest `sha256:8e575a37416d80380e4828984f759f166067063e7ab8b0894d9fb805f8608b5e` (not APK SHA).
- Explicit reviewed PR #99 promotion, verified publisher run `37938990631` PASS; public [v0.5.87-debug](https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.87-debug) contains unchanged stable-signed APK and `.apk.sha256`. Original **APK SHA256 `60795a057ad77b96df0f914a8c9341222d9684fc0087e89d9389c558761286f5`**.
- **Phone QA pending, issue #51 OPEN.** Original NT8298A ZIP itself was not uploaded to CI, so the fix has not been validated against those exact bytes. Next user action is a single in-place APK update and retry of same ZIP once, then screenshot of result. No user files touched by build/publishing.

## Real original NT8298A ZIP — phone acceptance (2026-10-09)

Following the debug prerelease, user tested the original 72,693,952-byte nested NT8298A ZIP and provided **`COMPLETE`**, 319 native sections, Package ID `megane-ii-nt8298a-2005-11-28`, generated .rdpkg SHA256 `e7fdbea2d3363af3ea3710eda22dcee518e36d08963b3483f0610a55602f6603`, runtime 119 seconds. **The exact prior nested ZIP regression is PHONE PASS**. This digest is for the generated .rdpkg, **not** for the signed Android APK. Other #51 acceptance remains open. UI jitter of progress detail has been registered as #100 (no code changes requested). Delete/file/data semantics are audited read-only in `qa/DELETE_BEHAVIOR_READONLY.md`.
