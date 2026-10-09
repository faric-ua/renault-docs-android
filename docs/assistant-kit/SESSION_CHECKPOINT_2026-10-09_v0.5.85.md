# Renault Docs handoff — v0.5.85/build101 — 2026-10-09

## Most recent actual app source and release

- User requested development on issue #51 after accepting installation of v0.5.84/build100 (over-existing install/data-retention phone PASS). **Do not assume v0.5.85 installed**.
- PR #94 MERGED, original application source SHA **`5b27f30ce92aed00e5b1e0e233d9a312a064bfb6`**. Archive intake ZIP/7Z/RAR already implemented, with prior multi-volume chooser, source read-only SAF copy, private extraction, duplicate fast-path and partial completion.
- v0.5.85 hardens (1) empty relativePath from a raw INDEX at archive root in mixed-root chooser/resume without path traversal, (2) root source identity based on original archive name rather than private `extracted` folder, threaded into compiler/duplicate preflight/output naming/chooser, (3) ambiguity/case-fold/file-directory collision rejection during ZIP, 7Z, RAR inspection and extraction before overwriting staged source bytes. Added Kotlin and Python contracts.
- PR Python Tests #586 PASS, Android PR Check #464 PASS; main Tests #587 PASS, stable-signed Android APK #148 PASS, workflow run ID **`37871352918`**. Original CI artifact ID **`11590002768`**, bundle digest `sha256:c0c1d8c73c04eed467c83ad1e0c9b59e0c5b3df70437a99c4ac0abb978cbf394` (NOT the inner APK SHA).
- PR #95 merged explicit verified promotion: GitHub Release publisher run **`37871568678`** PASS. Published **public QA-pending developer-signed debug prerelease**: https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.85-debug. Contains original signed APK + .apk.sha256. Exact APK SHA-256 **`dde09bde484d0f3d7392505a999faa8f8036a2ce38778c654fcbeac676baacb6`**.
- The previous v0.5.84 phone PASS covered only update/install/launch/project-volume retention; #40 background redelivery runtime QA remains open. All extended user phone QA is **PAUSED on user's request**, not deemed PASS/FAIL.

## Current issue #51 status and next engineering concern

**#51 remains OPEN** after source hardening. User's actual legacy archive formats, mixed-root multi-volume output isolation (a root candidate may contain nested independent raw volume), nonduplicate ZIP/7Z/RAR install, cancellation cleanup, encrypted/unsupported errors, lock/unlock, and exact source preservation are not independently phone accepted. The source-level fix does not prove root-plus-nested conversion isolation: do NOT call all of #51 complete. See:
- `docs/v.0.5.85/RELEASE.md`
- `docs/v.0.5.85/qa/BUG_REGISTER.md`
- `docs/v.0.5.85/qa/PHONE_TEST.md`
- `docs/v.0.5.85/RELEASE_META.json`

Potential next engineering work within #51: examine root/nested raw folder overlap semantics and enforce single selected-volume isolation in native staging without deleting/renaming original archive. Check stale temporary staging cleanup after interrupted/failed batch operations, and preserve per-volume names/status. If user asks to resume device QA, use small non-destructive test inputs and separate PASS by scenario.

## Rules

- Do not uninstall/reset Android app, clear data, delete originals (ZIP/7Z/RAR/.rdpkg), rename/move registered volumes, or start imports just to test.
- Keep Classic authentic, Modern responsive navigation intact.
- No arbitrary Termux widget shortcut edits; verified APK retrieval is Renault menu 5 → 19 → 8 → 13, or public signed GitHub Release when needed.
- **No phone QA prompts unless user wants to resume**. #51 and #40 remain open. Next distinct big project direction after #51 is #30 Windows source→prepared package/catalog pipeline.
