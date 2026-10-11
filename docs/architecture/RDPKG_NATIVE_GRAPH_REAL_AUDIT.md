# Renault Docs #30 — Stage I: prepared Modern/Runtime/Fast Pack consistency

## Live byte audit — 2026-10-11

After five original Windows ZIP→catalog-referenced .rdpkg real data fidelity PASS in Stage H, independently inspected the **generated** data within those same five full NT volumes. This checks a separate conversion result; HTML/PDF file parity alone cannot prove the native app can find a compiled section.

| Distinct NT | Modern sections | Runtime section shards parsed | Fast Pack files matched byte-for-byte | Compiler warnings | Unsupported actions |
|---|---:|---:|---:|---:|---:|
| NT8340A | 347 | 347 | 6,138 | 0 | 0 |
| NT8341A | 328 | 328 | 5,996 | 0 | 0 |
| NT8342A | 349 | 349 | 6,249 | 0 | 0 |
| NT8344 | 351 | 351 | 6,432 | 0 | 0 |
| NT8393 | 351 | 351 | 8,986 | 0 | 0 |
| **Total** | **1,726** | **1,726** | **33,801** | **0** | **0** |

For each exact matching .rdpkg, the real file audit checked:
- `_renault/modern-index.json` contains one current matching full NT volume.
- `_renault/modern-sections.json` Modern section codes, HTML entrypoints, Runtime IR index code maps, section-entry counters and coverage counts agree; every Modern HTML entrypoint exists in the package.
- All **1,726** sharded Runtime JSON files were found, read and parsed, with no missing paths or invalid JSON found.
- Generated documentation shard and Classic entrypoint paths exist.
- The nested Fast Pack ZIP bytes pass SHA-256 against the dataset manifest, its ZIP CRC and file-count check pass, and **each of the 33,801 Fast Pack entries** matches the same-named .rdpkg outer asset *byte-for-byte*, with 0 missing or modified.
- Compiler IR coverage declares **0 warnings and 0 unsupported actions** in these five real prepared packages.

No copyrighted ZIP/PDF data, internal Drive IDs, tokens or Drive URLs were added to the repository. Original and prepared Google Drive files unchanged. This is structural/data integrity, **NOT** an Android emulator test, live UI interaction, PDF render-diff, arbitrary legacy JavaScript execution or proof that schematics are technically correct.

## Reusable audit tool

```bash
python tools/audit_rdpkg_native_graph.py --package "/private/path/NT8340A.rdpkg"
```

This offline/read-only tool builds on Stage F's full .rdpkg CRC/payload validation and tests Modern/Runtime/coverage graph, source HTML references, indexed JSON shards and actual Fast Pack per-member byte identity. Result statuses:
- `PASS` if the graph and Fast Pack are consistent with no compiler warnings/unsupported actions.
- `REVIEW_REQUIRED` if compiler declares unsupported actions or warnings, without a structural error.
- `FAIL` if indexed section documents/shards are missing, mismatched NT/codes/counters, Fast Pack digest/CRC/assets differ or other structural problems exist.

Reports aggregate counts and problem codes, not original source contents. No extraction, writes, downloads, uploads or app changes. Synthetic tests exercise real-shaped graph and injected missing HTML, wrong section shard, corrupted Fast Pack with valid nested digest, wrong NT and declared unsupported actions. Require exact-head PR Python CI and main CI.

## Gate boundaries

- Scope: the **five** current Megane II catalog-referenced NT volumes, not all 24 original direct Megane ZIPs, 21 on-phone Megane tomes or the Laguna multi-volume source; continue #30 for remaining corpus.
- A PDF successfully opening and having a page count is not equivalent to visual quality verification.
- The fully independent signed/phone-accepted **CORE STABLE** release gate #147 remains open. Google login, free friend entitlements, Play payments and access encryption issue #133 remain strictly deferred to a **separate later release**.
