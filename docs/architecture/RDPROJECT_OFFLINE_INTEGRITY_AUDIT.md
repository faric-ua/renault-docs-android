# Stage J — full `.rdproject` integrity audit (offline, no source changes)

## Real Megane II phone export — 2026-10-11

- User created `Megane-II_X84-B84-C84-S84-E84-L84-K84.rdproject` with phone Renault Docs v0.5.97/build113; the in-app status said **`Проєкт Megane II підготовлено · 21 том`**.
- The complete file is visible in the user-owned `Renault Docs Projects / Phone exports - pending verification / Megane II` Google Drive folder. Exact Drive object is confirmed in that folder at **1,262,821,306 bytes**. **This proves upload/file metadata, NOT internal file validity or independent 21-volume audit.**
- Live connected Google Drive `fetch(download_raw_file=true, include_base64=false)` could not download the archive: **HTTP 413: 1,262,821,306 bytes exceeds connector limit 268,435,456 bytes (256 MiB)**. Do NOT claim all 21 volumes PASS; this archive's *content* remains **PENDING / UNVERIFIED** until a suitable read-only access path exists.
- No user Drive file was modified, moved, repackaged, deleted or made public; file stays in its separate intake location. Existing catalog of 5 Megane/10 Laguna packages stays untouched.

## New offline validator

`tools/audit_rdproject.py` is designed for a **locally accessible copy** of an existing `.rdproject`:

```bash
python tools/audit_rdproject.py --file "/path/to/Megane-II.rdproject" --expected-volumes 21
```

- Require a valid `renault-docs-project` schema v1 and `rdproject.json` with project metadata and **21 explicitly listed** volumes.
- Require exact equality of listed `volumes/*.rdpkg` files and actual outer ZIP contents; reject duplicates, traversal, symlinks, unexpected files and encrypted entries.
- Stream one embedded `.rdpkg` at a time through ZIP CRC (a temporary local file is removed after each volume), validate its schema and one-volume manifest, original project affiliation, complete NT/date identity, payload file count and declared uncompressed bytes. Never interpret neighboring NT codes as duplicates.
- Print JSON with each package's name, code/date, SHA-256, payload file counts, CRC and total volume count. An invalid archive returns **BLOCKED**; do not infer other rows PASS after failed or incomplete audit.
- **This is integrity/identity QA only:** it does not run Android UI, visually render PDFs, prove all generated Modern sections work or check byte parity with original Windows ZIPs. Those require separate documented checks.

Temporarily spooling a nested .rdpkg to system temp uses additional scratch space for **one volume**, never permanently extracts all 21 packages. No writes to input or Drive.

Synthetic CI scenarios test valid different NTs, incorrectly substituted NT, unexpected unlisted file, corrupt inner ZIP, wrong requested volume count and unchanged source/input.

## Follow-up / what counts as accepted

When the full original `.rdproject` becomes locally readable in the execution environment, run this validator and record its exact per-package result in `docs/assistant-kit/VERIFICATION_LEDGER.md`. Until then **only Drive presence/size is confirmed**, no internal PASS. Avoid asking the owner to re-export already prepared 21 tomes, and do not substitute five independently verified Drive-catalog Megane volumes for the complete 21-phone-volume archive. Keep #30 and Core Stable #147 open, #133 authorization/payments separate later.
