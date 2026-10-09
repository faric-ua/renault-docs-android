# Renault Docs v0.5.85 / build 101 — Archive Intake hardening

Status: **PR #94 MERGED / PR CI PASS / MAIN CI PASS / SIGNED APK #148 PASS / VERIFIED PUBLIC DEBUG PRERELEASE PUBLISHED / PHONE QA PAUSED**.

## #51 scope

ZIP / 7Z / RAR, private SAF source copy, extraction limits, duplicate preflight, batch chooser, and native .rdpkg validation already existed prior to this patch. This release fixes source-level gaps rather than rewriting the pipeline:

- A Renault raw volume whose INDEX.HTM is at the **archive root** can now be selected even if other raw roots are nested: empty candidate relativePath is deliberately valid, while ../, absolute paths and missing roots still fail containment checks.
- Root-level volume identity is based on the archive's filename (not the private directory named `extracted`), retained through the chooser label, duplicate detection, canonical output name and native local preparation. Nested volume identity remains unchanged.
- ZIP/7Z/RAR inspection and extraction reject multiple entries that normalize to the same file, case-incompatible paths or file/directory overlaps. This protects original raw payloads from silent overwrite during app-private extraction; repeated explicit directory entries remain permitted.
- Added Kotlin unit cases with synthetic ZIP fixtures and Python source-level contracts.

## Boundaries

No existing installed volume, saved archive or Classic runtime is changed. Original source SAF archive is read-only. Existing batch cancellation semantics retain already completed volumes and clean current incomplete output/staging. **Do not claim full #51 phone acceptance**: on-device ZIP/7Z/RAR nonduplicate import, mixed root archive, lock/unlock/process death, storage-limit UX and encryption failure still need later QA. #51 stays open until proven.

## Verified build and promotion — 2026-10-09

- Android source commit `5b27f30ce92aed00e5b1e0e233d9a312a064bfb6`, PR #94 merged.
- Python PR Tests #586 PASS and Android PR Check #464 PASS; main Tests #587 PASS; developer-signed Android Debug APK #148 PASS (run ID `37871352918`).
- Original signed artifact `Renault-Docs-v0.5.85-Debug` (ID 11590002768); Actions bundle digest `sha256:c0c1d8c73c04eed467c83ad1e0c9b59e0c5b3df70437a99c4ac0abb978cbf394` **is not the APK checksum**.
- PR #95 explicit promotion, GitHub publisher run `37871568678` PASS. [v0.5.85-debug](https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.85-debug) contains original APK + `.apk.sha256`, exact APK SHA-256 `dde09bde484d0f3d7392505a999faa8f8036a2ce38778c654fcbeac676baacb6`.
- This is a **public signed debug prerelease**, not a phone-approved production release. No live archive conversions or user-data cleanup executed.
