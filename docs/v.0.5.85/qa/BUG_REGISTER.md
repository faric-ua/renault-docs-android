# v0.5.85 #51 — source findings

- **ARCH-ROOT-001 — CONFIRMED SOURCE BUG / PATCHED**. `buildArchiveCandidates` rejected `relativePath == ""` even though `findRenaultRawRoots` can discover INDEX.HTM directly at extraction root; old resume resolver also rejected root itself. Both now use the same constrained root resolver.
- **ARCH-ROOT-002 — CONFIRMED SOURCE BUG / PATCHED**. A root-level source was called `extracted` during local native staging and downstream identity. The origin filename is now used for the root volume, retaining NT identity when present.
- **ARCH-PATH-003 — SOURCE HARDENING / PATCHED**. Archive member paths were independently containment-checked but multiple normalized paths could refer to the same final file and overwrite it silently. ZIP, 7Z, RAR inspection/extraction now reject ambiguous collision/overlap paths before reuse.
- **OPEN #51**. Runtime/phone QA of mixed-root batch and nonduplicate ZIP/7Z/RAR still pending; no actual user archive touched. Android-specific dataSync quota and process interruption evidence remains tracked in #40.
