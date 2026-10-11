# Renault Docs #30 — Stage H: real original Windows ZIP ↔ .rdpkg audit

## Verified phone-independent real corpus — 2026-10-11

**Actual source:** five original Renault Megane II ZIP archives from owner's connected Google Drive and the five distinct, exact catalog-referenced `.rdpkg` packages for the same **full NT codes**. The bytes were downloaded only to an isolated temporary analysis workspace; **not** committed, uploaded publicly, republished, edited, renamed or deleted. The origin provider Drive IDs, file URLs, account data and copyrighted document contents are intentionally absent from this report.

| Distinct document | Original volume files | Identical in .rdpkg | PDF files opened | PDF pages (structural) | Resolved static HTML references | Dynamic suffix markers |
|---|---:|---:|---:|---:|---:|---:|
| NT8340A (2006-04-18) | 7,653 | 7,653 | 1,517 | 1,884 | 46,536 | 0 |
| NT8341A (2006-10-09) | 7,476 | 7,476 | 1,482 | 1,831 | 45,524 | 0 |
| NT8342A (2006-10-09) | 7,844 | 7,844 | 1,597 | 1,994 | 48,049 | 0 |
| NT8344 (2007-05-02) | 8,116 | 8,116 | 1,686 | 2,118 | 49,566 | 5 |
| NT8393 (2007-06-04) | 14,223 | 14,223 | 2,961 | 3,368 | 67,131 | 5 |
| **Total** | **45,312** | **45,312** | **9,243** | **11,195** | **256,806** | **10** |

### Verified from actual archive/package bytes

- All 5 original ZIPs and all 5 catalog-referenced prepared `.rdpkg` ZIPs passed full ZIP CRC.
- Every **source file** path (HTML/PDF/GIF/JS) exists in the selected matching prepared `.rdpkg` volume subtree. Original and packaged byte strings were compared **exactly**: **0 missing, 0 extra, 0 modified files** across all 45,312. The new derived `_renault/` files inside each `.rdpkg` are expected generated output and were not treated as additions to source documentation.
- Independently opened all **9,243 PDF files** from the prepared packages using PyMuPDF, read each page count (total **11,195**) with **0 errors reported by opening/parsing**. This is NOT a pixel-perfect visual check of every PDF page or verification that all technical statements are correct.
- Scanned literal HTML static file references (`.htm/.html/.pdf/.gif/.ico/.js`): **256,806** resolved to existing, exact-case files. The only 10 apparent unresolved references were legacy JavaScript concatenation suffix fragments such as `".HTM"` (5 per NT8344 and NT8393), **not literal missing files**; separated into `dynamic_suffix_marker`. No static missing targets or case mismatches in this audit scope.
- Each of the five full NT codes is separate, including **NT8340A ≠ NT8342A**. No guessed supersession, pairing by nearest document number, duplicate removal or overwriting of older Drive file objects (#143).

**Evidence boundary:** 5/5 current **Megane II catalog-referenced** source→package pairs only. Other original ZIP names in Drive (including 24 direct Megane schematics), Laguna packages sourced from a large multi-volume archive, Kangoo, 21 phone-installed Megane tomes, runtime Classic/Modern interaction, dynamic JavaScript navigation, and all possible generated IR/Fast Pack semantic links are NOT implied to pass. The original ZIP→.rdpkg parity passed **without any actual HTML normalization** for these five documents (all original files byte-identical); separate synthetic tests exercise legitimately normalized HTML and identify unexplained changes. No claim about public redistribution rights or final Core Stable acceptance.

## Reusable Stage H tool

`tools/audit_original_zip_rdpkg.py`:

```bash
python tools/audit_original_zip_rdpkg.py \
  --original "/private/path/original NT8340A.zip" \
  --package "/private/path/prepared NT8340A.rdpkg"
```

- Parses existing `rdpkg.json` to establish **one source-volume folder** and complete NT identity; refuses different NT codes/dates or multiple source roots. Rejects unsafe paths, duplicate/case-colliding ZIP members and symlinks.
- Runs full Stage F prepared ZIP payload CRC, original ZIP CRC and exact file-set parity.
- Every same-name original file must either have the **same bytes**, or, **only for .htm/.html/.js**, match the exact `core.convert_paths.patch_text()` allowed case/path normalization expected from the current fast converter. Missing/extra/changed PDF, GIF, etc. or unexplained HTML changes fail.
- Static reference scan is **diagnostic** (not browser/JS execution); explicitly recognizes old `".HTM"` concatenation markers to avoid false missing-file reports.
- Prints sanitized JSON to stdout, no private provider IDs, remote URL, file content or absolute file path; exits 0 on PASS, 2 on content FAIL. **No extraction, conversion, output writes, catalog edits, source Drive changes or phone actions.**

The five real downloaded pairs above were inspected with an independent local ZIP byte comparison, PDF parser, and static-link audit. CI synthetic tests exercise this repository tool with controlled normalized HTML+PDF/GIF source packages, cross-NT mismatches, extra/missing/modified files and no user data.

## Remaining acceptance gates

- Expand actual source corpus coverage beyond these five matching Megane pairs using explicit provider folder/model/NT provenance and safe access; large Laguna multi-volume originals need separate volume discovery.
- Audit generated Modern/Runtime/Fast Pack content and Android viewer/navigation semantics independently of file parity; perhaps include sampled page rendering for critical diagrams.
- Record true coverage rather than call all Renault volumes tested; #30 and **Core Stable #147 remain OPEN**.
- #133 friend accounts, billing and access/security belongs to an entirely separate release **after** owner accepts independently working Core Stable.
