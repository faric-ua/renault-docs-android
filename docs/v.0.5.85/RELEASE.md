# Renault Docs v0.5.85 / build 101 — Archive Intake hardening

Status: CODE CANDIDATE / CI PENDING / PHONE QA PAUSED BY USER.

## #51 scope

ZIP / 7Z / RAR, private SAF source copy, extraction limits, duplicate preflight, batch chooser, and native .rdpkg validation already existed prior to this patch. This release fixes source-level gaps rather than rewriting the pipeline:

- A Renault raw volume whose INDEX.HTM is at the **archive root** can now be selected even if other raw roots are nested: empty candidate relativePath is deliberately valid, while ../, absolute paths and missing roots still fail containment checks.
- Root-level volume identity is based on the archive's filename (not the private directory named `extracted`), retained through the chooser label, duplicate detection, canonical output name and native local preparation. Nested volume identity remains unchanged.
- ZIP/7Z/RAR inspection and extraction reject multiple entries that normalize to the same file, case-incompatible paths or file/directory overlaps. This protects original raw payloads from silent overwrite during app-private extraction; repeated explicit directory entries remain permitted.
- Added Kotlin unit cases with synthetic ZIP fixtures and Python source-level contracts.

## Boundaries

No existing installed volume, saved archive or Classic runtime is changed. Original source SAF archive is read-only. Existing batch cancellation semantics retain already completed volumes and clean current incomplete output/staging. **Do not claim full #51 phone acceptance**: on-device ZIP/7Z/RAR nonduplicate import, mixed root archive, lock/unlock/process death, storage-limit UX and encryption failure still need later QA. #51 stays open until proven.
