# Renault Docs v0.5.94 — one-level nested ZIP batch

## User-provided structure

A single `Megane IIx.zip` in `Documents/Renault/Megane II` contains three true compressed `.zip` files, named approximately for NT8266, NT8228A and NT8274. Each inner ZIP contains a normal subfolder and then `index.html` (`outer.zip → inner.zip → folder → index.html`). v0.5.92 returned no raw-root because it only inspected ordinary paths of outer ZIP.

## Source implementation

- Leave the existing direct `INDEX.HTM/INDEX.HTML/ACCUEIL.HTM` raw-root discovery path unchanged.
- When **outer extraction contains no raw roots**, allow only the dedicated outer staged extraction to succeed without them, then discover inner compressed ZIP entries (not resource archives within valid raw volumes).
- Extract at most **one** nested ZIP level into private `noBackupFilesDir/archive-intake/…/extracted/__renault_nested_zip_roots`, with independent archive subfolders; find entrypoints under each and hand the union to the established multi-root chooser.
- Preserve input archives read-only. Validate each inner archive signature, ZIP entry paths (Zip Slip/case collisions), safe-root containment, cumulative max 20 inner ZIP files, max 768 MiB compressed source, max 8 GiB expanded including the outer archive, max 200000 entries, 128 MiB free-space reserve, max 16 directory levels scanned. Check cancellation while copying every buffer. Do not recurse into a third compression level; reject with explicit message.
- Existing chooser and duplicate detection still control what gets installed; no silent default import of all three and no automatic edits to existing tomes.
- Explicitly distinguish nested 7Z/RAR (unsupported in this incremental release) from nested ZIP, without erroneously claiming to support all format combinations. Top-level ZIP/7Z/RAR remains unchanged.

## QA

Kotlin tests generate an outer ZIP containing three inner ZIP files, each with `folder/index.html`, and verify three distinct detected roots, as well as Zip Slip rejection, no arbitrary deep recursion and cancellation. Python source contracts cover integration with chooser. **Android CI, stable-signed APK and actual device QA still pending.** No claim that user's real archives were accessible to CI.

Unrelated v0.5.93 notification-history upgrade resilience is separate and does not regenerate older already-lost notification messages.
