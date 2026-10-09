# Renault Docs — handoff v0.5.87 / build 103 — 2026-10-09

## Actual phone bug and fix

User installed v0.5.86 and tried to convert `Megane II B,C,S 84_NT8298A_Visu v3.0_2005.11.28.zip` (72,693,952 bytes) in project Megane II. Project had 10 volumes at the time. Preflight source/project name PASS; after output-folder selection, actual conversion **FAILED**: `Вибрано батьківську або змішану папку`. Built-in read-only diagnostics: `sourceKind=ARCHIVE_FILE`, phase FAILED, no package ID or SHA. User's archive manager screenshot shows an extra wrapper folder(s) and inside `INDEX.HTM` (~653 bytes), `COMMUN/`, `RUS/`. Original full SAF URI/document ID is **not reproduced in public GitHub docs**. Exact failed extracted root wasn't persisted; root cause at path handoff is not proven from existing report alone.

### v0.5.87 implementation

- PR #98 merged runtime app source `937ffa06080c3dd6a63a87d4e9c5d209ff4b8755`, versionName 0.5.87, versionCode 103.
- `ArchiveNativeRawRoot.resolve()`: before local native scanning, retain an exact direct-entrypoint root; otherwise descend through wrapper(s) **only if one nested raw root is independently detected**. Refuse zero or several (no arbitrary selection). Strict canonical containment; no source changes.
- For a batch chooser with explicitly excluded nested candidate folders, preserve the chosen root and fail rather than auto-reselect another volume. v0.5.86 isolation remains.
- If native scanner still loses direct INDEX, report bounded relative top-level filenames and detected INDEX candidates, not full SAF path, to permit decisive next-step diagnosis.
- JUnit synthetic ZIP mimics nested `INDEX.HTM`, `COMMUN/`, `RUS/`, checks `ArchiveIntake.extract` → `ArchiveNativeRawRoot.resolve` → exact native `hasRootEntrypoint` gate, plus ambiguous/missing cases. Python source contract included.
- Python PR Tests #591 PASS; Android PR Check #467 PASS, main Tests #592 PASS; stable-signed Android Debug APK #150 PASS, run `37938581867`.
- Original APK Actions artifact ID `11620685925`, bundle digest `sha256:8e575a37416d80380e4828984f759f166067063e7ab8b0894d9fb805f8608b5e`.
- PR #99 merged exact reviewed promotion; publisher run `37938990631` PASS. Public developer-signed debug prerelease [v0.5.87-debug](https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.87-debug), APK and independent sha256. Actual APK SHA256 `60795a057ad77b96df0f914a8c9341222d9684fc0087e89d9389c558761286f5`.

## STOP AT USER ACTION — NOT AN ENGINEERING BLOCKER

All engineering, CI, signature, release and documents are complete. Next **single user-controlled device check**: install v0.5.87/build103 over currently installed app using Renault Termux 5→19→8→13, without uninstall or clearing data. In Megane II, convert **the same original NT8298A ZIP once**, use intended output folder, and report final Completed/Failed screen plus updated count (previously 10). On failure, screenshot bounded diagnostics. DO NOT blindly repeat conversion, delete/rename source ZIP, or clear existing tomes.

**Issue #51 OPEN until this exact real-phone success/diagnosis**, even though source/CI PASS. **#40** separate background lock/process restart runtime QA still OPEN. No manual phone test, installation or original archive access performed by assistant.

Canonical files `docs/v.0.5.87/RELEASE_META.json`, `docs/v.0.5.87/qa/PHONE_TEST.md`, `docs/v.0.5.87/qa/BUG_REGISTER.md`. After user PASS, record only scopes actually observed.


## Real NT8298A ZIP acceptance and new no-code findings — 2026-10-09

- User completed actual original Megane II archive `Megane II B,C,S 84_NT8298A_Visu v3.0_2005.11.28.zip` (72,693,952 bytes) through app native `.rdpkg` flow. **COMPLETE**, 319 native sections, label `NT8298A · 2005-11-28`, package ID `megane-ii-nt8298a-2005-11-28`, original generated package SHA256 `e7fdbea2d3363af3ea3710eda22dcee518e36d08963b3483f0610a55602f6603`, elapsed 119 seconds (16:57:20–16:59:19 phone local). **PHONE PASS for the real nested NT8298A ZIP prior FAIL**; completed code path includes app-private install and upsert into Megane II before status COMPLETE. The user did not separately show project total changing or opening all pages. The result SHA256 is not the signed APK SHA256.
- Live status line was observed jumping as `копіюю/готую/пакую` and processed files counters alternate, sometimes only counts. **UX issue #100 filed**, record-only, **no UI code change** per explicit user request; issue #25 prior global progress work was already closed.
- Asked about deletion across Megane II/Kangoo II and potentially same name. **Read-only audit** `docs/v.0.5.87/qa/DELETE_BEHAVIOR_READONLY.md` documents:
  - `ProjectStore.removeVolume(projectId, volumeId)` removes one project's association, may invalidate prepared project-share cache, but does not delete installed dataset or external archive/RDPKG.
  - `PreparedShareStore.deleteVolume()` can remove canonical + legacy **private prepared share copies** for one volume (zero to two), not source/external package, so "two files" could refer only to cache naming here, not two projects' tomes.
  - New native package IDs include projectId, and generated canonical filenames include model, so Megane/Kangoo newly generated packages are normally distinguishable. Exact physical filenames/URIs of existing volumes remain unverified without on-device read-only inspection.
  - Android Settings clear-data wipes **all** project metadata/app-private extracted packages, not just a selected project. Do not test by deleting/clearing data.
- #51 remains OPEN for other archive/7Z/RAR/batch/cancel scenarios despite this ZIP PASS. #40 remains OPEN for runtime background QA. No new build, code modifications, deletion or re-import initiated this turn.
