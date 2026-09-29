# v0.5.51 Phone test — Kotlin-native raw Renault → .rdpkg

Status: **PHONE PASS — 2026-09-28**

Reference volume:
`NT8340A · 2006-04-18`

Reference result:
`347 · native`

This QA covers the Android/Kotlin **raw-folder converter**. It is separate from the already closed v0.5.50 managed-package export/import round-trip.

## Gate A — raw source / output boundary

Result: **PASS**

Verified:
- exact raw NT8340A volume folder selected;
- output chosen as a final `.rdpkg`, not a public intermediate dataset;
- mixed/parent-folder selection is rejected by the fixed candidate;
- destination-inside-source is rejected;
- source remains read-only.

## Gate B — lifecycle

Result: **PASS**

Regression being verified:
a stale Activity-result replay had previously been able to start a second conversion after the first run completed.

Fixed candidate:
- one-shot request UUID guards the source→destination start;
- duplicate delivery of the same request cannot launch a second run.

Phone evidence:
- post-completion: rotation, leaving Renault Docs and switching between apps did not restart conversion;
- active PREPARING: rotate portrait → landscape → portrait, background the app, switch to another app and return; the same copy operation continued
  `500/7653 → 1400/7653 → 2300/7653`
  through rotation/background/external-app handoffs;
- no reset to a fresh scan;
- no duplicate/parallel run.

Disposable Cancel check:
- Cancel during PREPARING succeeded;
- source remained unchanged;
- private staging was cleaned.

## Gate C — package generation / validation / install

Result: **PASS**

Observed:
- source files copied: `7653`;
- Runtime IR: `347` sections;
- Fast Pack: `6138` files;
- outer `.rdpkg`: `8012` files;
- app automatically ran `RdpkgImporter.install()`;
- validation/import completed;
- existing NT8340A was upserted into the same Megane II project;
- project stayed at exactly 2 volumes, so no duplicate NT8340A was created.

Generated package SHA-256:
`7d6cc758b329adcd1ef46680de4d9c6815ff48b053f4e3550b06dd92b229b77e`

The same SHA-256 was observed on successful reruns, providing deterministic-output evidence.

## Gate D — reopen parity

Result: **PASS**

Verified on phone:
- title: `NT8340A · 2006-04-18`;
- Modern: `Розділів: 347 · native`;
- no `Сумісний режим` / compatibility fallback;
- Classic remains available only as the explicit alternate mode.

## Gate E — storage contract

Result: **PASS for the current v0.5.51 Kotlin flow**

Verified:
- generated `.rdpkg` exists at the chosen destination;
- current NT8340A Kotlin-native run created no new public `*_android` intermediate;
- raw source was not modified.

Historical user-visible folders already present under `Documents/Renault`:
- `laguna 2 2001-2006_android`;
- `Megane II_android`;
- `Megane II_NT8342A_android`.

These are legacy artifacts from earlier workflows and are tracked separately for a read-only provenance/reference audit. They are not evidence of the accepted v0.5.51 flow creating a new public intermediate.

## Candidate evidence

Phone-accepted runtime source:
`6b334eafb1fbf9dcca591fa4d1b619885f32ea4b`

CI:
- Tests `36370544159` — PASS;
- Android Debug APK `36370544247` — PASS.

Artifact:
- `Renault-Docs-v0.5.51-Debug`;
- id `10948508448`;
- APK SHA-256 `7b51c30cdd0be50de48ff8ddc21c6ee85f33c00eaa9c93adacf22fec4ef5ab48`.

## Final verdict

All required phone gates for raw NT8340A → Android/Kotlin → `.rdpkg` → validation/import → native reopen are satisfied.

**v0.5.51 PHONE PASS — 2026-09-28.**

Non-blocking follow-ups:
- terminal COMPLETE/CANCELLED/FAILED status should have a dismiss `×`;
- Ukrainian singular wording should show `1 файл`, not `1 файлів`;
- audit historical public `*_android` folders before cleanup.
