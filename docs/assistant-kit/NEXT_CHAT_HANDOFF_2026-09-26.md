# Next-chat handoff — Renault Docs

Date: 2026-09-26
Purpose: exact recovery point for the next chat/session.

## Current accepted baseline

### PDF / Companion
v0.5.36 / versionCode 52 is PHONE PASS.

BUG-006 is CLOSED.

Confirmed on phone:
- fullscreen survives portrait/landscape rotation;
- main app toolbar stays hidden while fullscreen;
- PDF fullscreen button remains visually synchronized with Android fullscreen state;
- one tap exits fullscreen correctly;
- one-document and two-document fullscreen flows are accepted.

Do not continue polishing PDF/Companion unless a new real-phone bug appears.

Merged v0.5.36 main source:
`3b6580d5a2b8d2a012a978cd828c220035dbef79`.

---

## Current active stage

### v0.5.37 — Converter Writer Wave 1

Status:
**MERGED · CI PASS · PHONE TEST PENDING**

Version:
- versionName `0.5.37`
- versionCode `53`

Branch used:
`feat/v0.5.37-converter-writer`

Merged main commit:
`51c2d77771a7c5676e5793ee38de2f180eaf7bb5`

Green tested source:
`de28d445749c752d5d0d1ecbf31959cab92b8d3f`

CI:
- Tests run `36209095449` — PASS
- Android Debug run `36209095432` — PASS
- artifact: `Renault-Docs-v0.5.37-Debug`
- artifact id: `10894143612`
- APK SHA-256:
  `ac7b0e6dfe0227c86b536f1811249690e8e6fe625f5364354c9ce81ccc590e6e`
- artifact ZIP SHA-256:
  `bed2177788448146fd3ac96f0f955a69922256bc3fd11c948463221d661f9739`

## What v0.5.37 actually implements

This is the first real Android converter execution wave.

Implemented:
- source SAF folder picker;
- destination-parent SAF folder picker;
- persistent conversion draft;
- real `Почати конвертацію`;
- foreground `dataSync` ConversionService;
- persistent ConversionRunStore;
- screen rotation / leave-return reconnects to the same run instead of intentionally restarting;
- recursive source scan;
- complete exact-path map before patching;
- hidden staging folder:
  `.<source>_android.renault-staging`;
- recursive SAF copy;
- existing Kotlin `ConverterPathNormalizer` applied to HTM/HTML/JS;
- ISO-8859-1 byte-preserving legacy text handling;
- conversion-report.json;
- base package generation;
- output validation before final rename;
- staging -> `<source>_android` only after successful validation;
- cancel request + pre-finalization staging cleanup;
- completed output can be registered in Library;
- existing final output is never silently overwritten;
- destination inside source is rejected.

Critical safety:
**v0.5.37 never deletes or modifies the original source folder.**

If conversion fails/cancels before finalization:
- staging is removed;
- source remains untouched.

## v0.5.37 package boundary

Wave 1 creates:
- `renault-dataset.json`
- `conversion-report.json`
- `_renault/START.html`
- `_renault/README_UA.html`
- `_renault/volumes.json`
- `_renault/modern-index.json`

Important:
v0.5.37 is NOT the final Converter 2.0.

It does NOT yet generate:
- `modern-sections.json`;
- Runtime IR v2 section shards/index;
- per-volume documentation shards;
- Runtime IR coverage;
- Fast Pack.

Therefore v0.5.37 output is currently:
**normalized / Classic-ready + volume-level Modern catalog**.

---

## Immediate next action in the next chat

The user has NOT yet reported the v0.5.37 phone result.

First task:
perform v0.5.37 phone validation.

Preferred first test:
use a small copied Renault source/subset if practical before running the full 61k+ file corpus.

Test sequence:
1. Install exact v0.5.37 APK/artifact.
2. Open Library -> `Конвертувати стару папку`.
3. Select OLD Renault source.
4. Select destination parent, e.g. `Documents/Renault`.
5. `Перевірити план`.
6. Start conversion.
7. During conversion:
   - rotate device;
   - leave app;
   - return;
   - confirm same run/progress continues.
8. Cancel once during copy.
9. Confirm:
   - staging disappears;
   - final output does not appear;
   - source is unchanged.
10. Run again to completion.
11. Confirm output:
    `<destination>/<source>_android`.
12. Confirm no stale `.renault-staging`.
13. Tap `Додати готову папку в бібліотеку`.
14. Confirm Library validation succeeds.
15. Try same conversion again and confirm existing output is refused, not overwritten.

Do NOT mark v0.5.37 PHONE PASS until the user explicitly confirms.

---

## Important APK handoff issue

Renault Menu point 8 currently has a known artifact-selection problem.

Observed:
when user expected v0.5.36, point 8 downloaded v0.5.35.

Until fixed/verified:
**do not tell the user to use point 8 for the active phone gate.**

Use the exact artifact id / exact APK instead.

Current exact v0.5.37 artifact:
`10894143612`.

A future cleanup task is to fix point 8 so it selects the exact artifact recorded in current release metadata rather than an older/latest-matching build.

---

## Next development wave after v0.5.37 PHONE PASS

### v0.5.38 — Full Android Modern compiler parity

Goal:
make the in-app converter produce the same full Modern dataset that the current Python/package pipeline produces.

Port / reproduce in Android:
1. full Classic catalog discovery;
2. `modern-sections.json`;
3. Runtime IR v2 compiler;
4. Runtime IR section shards/index;
5. per-volume documentation shards;
6. Runtime IR coverage;
7. Fast Pack generation;
8. final package manifest metadata.

Then run a full parity audit against the existing Python/package output.

Do not call Converter 2.0 complete before this parity wave passes.

---

## Converter parity rules that must be preserved

Renault section/connector IDs are opaque identifiers.

Never infer semantic meaning from suffixes like `_1`, `_2`, `_3`.

Examples:
- section 101 variants may represent applicability differences;
- ECU block 120 variants may represent physically separate connectors.

Preserve full IDs exactly:
- `101`
- `101_1`
- `101_2`
- `120_1`
- `120_2`
- `120_3`
etc.

Classic catalog parity must preserve:
- source order;
- non-3-digit IDs;
- mixed IDs such as `1405`, `R325`, `MAH`, `MYH`, `NT`, `NU`;
- duplicate display codes when they point to different targets;
- deduplicate only same display code + same target.

BUG-004 is considered implemented/closed in the current Python/package/runtime architecture and must not regress during Android compiler port.

---

## Volume documentation rule

Documentation is per-volume / per configuration.

Do NOT merge documentation across volumes.

Within one volume the documentation bundle is shared for that volume and includes categories such as:
- Запобіжники;
- Довідка;
- Загальна документація.

The existing app already uses volume-level documentation and PDF Companion.

Future compiler cleanup may remove duplicated `GENE / PLATFUSI / AIDE` document graph data from every section shard only after Android compiler parity proves volume-level documentation is sufficient.

---

## Product direction after full compiler parity

After full Modern compiler parity:
1. verified backup of original source;
2. original-source ZIP;
3. metadata JSON;
4. SHA-256;
5. archive read/verification;
6. only then explicit final user confirmation to delete source;
7. Backup Center:
   - verify;
   - restore;
   - show location;
   - delete backup.

Critical invariant:
source deletion is impossible if conversion, validation, backup creation, backup verification or final confirmation fails/cancels.

After Converter 2.0 is complete for Laguna II:
use Megane II as the first real new dataset to prove the architecture is not Laguna-II-hardcoded.

---

## Current project strategy

Do not return to PDF polish unless a real bug appears.

Priority order:
1. v0.5.37 phone gate;
2. v0.5.38 full Android compiler parity;
3. full-corpus parity audit;
4. verified backup + deletion confirmation;
5. Backup Center;
6. Megane II external dataset validation.

Repository files to read first in a new session:
- `CURRENT_HANDOFF.md`
- `docs/assistant-kit/PROJECT_LEDGER.md`
- `docs/assistant-kit/OPEN_FINDINGS.md`
- `docs/v.0.5.37/RELEASE.md`
- `docs/v.0.5.37/qa/PHONE_TEST.md`
- this file.

Do not rely on chat memory alone; repository is the source of truth.
