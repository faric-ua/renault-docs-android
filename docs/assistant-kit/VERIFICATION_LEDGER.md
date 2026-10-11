# Renault Docs — completed validation ledger / реєстр PASS

Updated: **2026-10-11**. This is the canonical checklist for **previously completed** core documentation-conversion QA. Link from [issue #30](https://github.com/faric-ua/renault-docs-android/issues/30) and the assistant's current handoff. Checkmarks mean **the described, bounded acceptance was completed**, never blanket acceptance of all Renault files or Android flows.

## Automated tests in repo — completed milestones

- [x] **Stage A — Windows archive source discovery / metadata inventory.** Offline inventory tool and tests `tests/test_windows_source_archive_inventory.py`. **Source-code tool & synthetic tests verified**; does not validate every original archive's content.
- [x] **Stage B — Drive prepared-catalog metadata collision audit.** `tests/test_drive_catalog_metadata_audit.py`, PR [#142](https://github.com/faric-ua/renault-docs-android/pull/142), exact PR Python Tests **#649 PASS**, main **#650 PASS**. Checks selected Drive IDs/name/size and duplicates; no archive bytes inferred.
- [x] **Stage C — known Drive original source folders first-layer inventory (read-only snapshot).** Kangoo II, Megane II, Laguna II bounded source lists recorded under [#30](https://github.com/faric-ua/renault-docs-android/issues/30) and [PR #145](https://github.com/faric-ua/renault-docs-android/pull/145). **Metadata-only**, not complete recursive corpus.
- [x] **Stage D — exact source-folder allowlist.** `tests/test_drive_source_metadata_allowlist.py`, PR [#146](https://github.com/faric-ua/renault-docs-android/pull/146), exact PR Python Tests **#651 PASS**, main **#652 PASS**. Preserves separate full NT identities.
- [x] **Stage E — exact Drive-ID prepublish local/package metadata preflight.** `tests/test_preflight_drive_publish_plan.py`, PR [#149](https://github.com/faric-ua/renault-docs-android/pull/149), exact PR Python Tests **#653 PASS**, main **#654 PASS**. Does not grant automatic publication.
- [x] **Stage F — full local .rdpkg ZIP CRC / manifest / entry-safety verification.** `tests/test_rdpkg_payload_integrity.py`, integrated Stage E check, PR [#151](https://github.com/faric-ua/renault-docs-android/pull/151), exact PR Python Tests **#655 PASS**, main **#656 PASS**.
- [x] **Stage G — normalized prepared volume HTML/PDF vs .rdpkg SHA-256.** `tests/test_prepared_volume_rdpkg_parity.py`, PR [#153](https://github.com/faric-ua/renault-docs-android/pull/153), exact PR Python Tests **#657 PASS**, main **#658 PASS**. **Synthetic** real-converter fixtures, not the entire personal Drive corpus.
- [x] **Stage H — ORIGINAL Windows ZIP ↔ prepared .rdpkg fidelity**, limited to the 5 current Megane II catalog volumes. `tests/test_original_zip_rdpkg_audit.py`, PR [#155](https://github.com/faric-ua/renault-docs-android/pull/155), exact PR Python Tests **#660 PASS**, main **#661 PASS**. Earlier #659 failed due to a test-auditor `.HTM` regex and was corrected before merge. Real evidence below.
- [x] **Stage I — generated Modern / Runtime IR / Fast Pack integrity**, limited to the *same five* Megane II packages. `tests/test_rdpkg_native_graph_audit.py`, PR [#156](https://github.com/faric-ua/renault-docs-android/pull/156), exact PR Python Tests **#662 PASS**, main **#663 PASS**. Real evidence below.

All committed automated tests **remain in `tests/`**, so subsequent CI may run them again as **regressions** when implementation changes. A regression run is not a request for the owner to manually re-check the same accepted documents.

## Real Drive source/package verification — 5 separate accepted NTs

Every row below reflects an independently inspected **original ZIP ↔ the exact current catalog-referenced .rdpkg** pair, not comparison of neighboring NT documents. All read-only; no Drive file modifications, deletions, duplicate cleanup or copyrighted file bytes committed.

| Full NT identity | Raw source file fidelity | Structural PDF open/parse | Static HTML reference scan | Modern/Runtime index & Fast Pack |
|---|---|---|---|---|
| **NT8340A** | [x] PASS — 7,653 / 7,653 | [x] PASS — 1,517 PDFs | [x] PASS — 46,536 exact targets | [x] PASS — 347 sections / 6,138 Fast Pack assets |
| **NT8341A** | [x] PASS — 7,476 / 7,476 | [x] PASS — 1,482 PDFs | [x] PASS — 45,524 exact targets | [x] PASS — 328 sections / 5,996 Fast Pack assets |
| **NT8342A** | [x] PASS — 7,844 / 7,844 | [x] PASS — 1,597 PDFs | [x] PASS — 48,049 exact targets | [x] PASS — 349 sections / 6,249 Fast Pack assets |
| **NT8344** | [x] PASS — 8,116 / 8,116 | [x] PASS — 1,686 PDFs | [x] PASS — 49,566 exact targets | [x] PASS — 351 sections / 6,432 Fast Pack assets |
| **NT8393** | [x] PASS — 14,223 / 14,223 | [x] PASS — 2,961 PDFs | [x] PASS — 67,131 exact targets | [x] PASS — 351 sections / 8,986 Fast Pack assets |
| **TOTAL** | **45,312 / 45,312; 0 missing/extra/changed** | **9,243 PDFs, 11,195 pages; 0 structural parsing errors** | **256,806 exact targets; 0 missing literal targets** | **1,726 sections/shards; 33,801 matching Fast Pack files; 0 compiler warnings / unsupported actions** |

Ten historically constructed dynamic JavaScript `.HTM` extension fragments (5 in NT8344, 5 in NT8393) were excluded from the **literal static link** category; they were not missing files. PDF PASS = parser opens/recognizes page tree, **not** visually inspecting all 11,195 pages. Modern/Runtime PASS = graph and data integrity, **not** interactive Android UI automation.

The detailed, separate evidence is preserved permanently:
- [Stage H real-source audit](../architecture/ORIGINAL_ZIP_RDPKG_REAL_AUDIT.md)
- [Stage I real-native graph audit](../architecture/RDPKG_NATIVE_GRAPH_REAL_AUDIT.md)
- [Issue #30](https://github.com/faric-ua/renault-docs-android/issues/30), [Core Stable gate #147](https://github.com/faric-ua/renault-docs-android/issues/147)

## STILL OPEN — do not mark PASS from the above

- [ ] Additional original Megane II ZIPs not represented by these 5 catalog pairs, independent files installed on the phone, and Kangoo II / Laguna II source/package corpus checks. Do not assume the 21 Megane II phone volumes are these five, or that there are only five available source archives.
- [ ] On-phone Classic/Modern navigation, complex JavaScript behavior and PDF visual rendering/UX acceptance for the eventual **exact signed CORE STABLE build**. File/graph parity alone does not prove UI behavior.
- [ ] Actual end-to-end Windows multi-volume source intake, conversion, content semantics, catalog publication controls and third-party redistribution rights. **#30 OPEN**.
- [ ] Remaining separately triaged phone/platform acceptance or justified deferral: #40 FGS Android timeout emulator test, #51 archive format/cancel edges, #85 prepared-package negative-picker guard, #102 notifications and #123 natural per-volume reports.
- [ ] Complete signed, source-frozen, exact-artifact regression-tested, on-phone owner-accepted **CORE STABLE** release. **#147 OPEN**.
- [ ] Friends/Google login/Google Play payment/content protection #133 is **BLOCKED UNTIL Core Stable**, intentionally excluded from all checks above.

## Reuse / don't repeat rule

1. **Do not request repeat manual inspection** of a PASS row solely because this chat ended, the plan moved forward or a neighboring NT code exists. The evidence is saved in this ledger, CI and PRs.
2. **Automatically rerun affected regression tests** when converter/parser/indexing code changes, or when a source ZIP or prepared package identity/bytes changes. Reassess that affected row only if the new content or code actually invalidates prior evidence.
3. For **new NTs**, run the same automated audits. Never infer an NT successor is interchangeable or a same-named Drive object is redundant (#143).
4. A failed or unrun scenario remains **OPEN / BLOCKED / REVIEW_REQUIRED** — never marked PASS for convenience. Only close parent issue #30 or Core Stable #147 when their remaining explicit acceptance criteria are satisfied.

**No new APK or source file modification was required to create this QA ledger.**
