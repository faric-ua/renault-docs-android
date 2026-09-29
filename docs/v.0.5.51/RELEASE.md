# Renault Docs v0.5.51 — Kotlin-native raw → .rdpkg

Status: **PHONE PASS / MERGE CLOSEOUT**

## Goal

Prepare one raw Renault volume entirely inside Android/Kotlin:

```text
raw Renault SAF folder
→ app-private normalized staging
→ Kotlin Modern compiler
→ Section IR v2
→ Runtime IR
→ Fast Pack
→ manifests
→ streamed .rdpkg
→ validation/import
→ project upsert
```

Python/Termux remain reference/developer tooling only and are not part of the production user flow.

## Implemented

- one metadata scan of the selected raw SAF source;
- one normalized copy into app-private staging;
- no user-facing `*_android` intermediate;
- single-volume source boundary validation;
- destination-inside-source rejection;
- Kotlin volume discovery / Modern index;
- native section discovery with opaque Renault IDs;
- Section IR v2;
- Runtime IR tree/shards/index/coverage;
- Fast Pack;
- `renault-dataset.json` and `rdpkg.json`;
- streaming outer package writer with SHA-256 during write;
- foreground service + wake lock;
- persistent run state and Activity reattachment;
- one-shot request-id protection against stale Activity-result replay;
- Cancel during PREPARING;
- automatic `RdpkgImporter.install()` validation/import;
- project upsert without duplicate volume;
- stale RUNNING recovery after process/service loss.

## CI / accepted candidate

Phone-accepted runtime source:
`6b334eafb1fbf9dcca591fa4d1b619885f32ea4b`

CI:
- Tests `36370544159` — PASS;
- Android Debug APK `36370544247` — PASS.

Artifact:
- `Renault-Docs-v0.5.51-Debug`;
- artifact id `10948508448`;
- APK SHA-256 `7b51c30cdd0be50de48ff8ddc21c6ee85f33c00eaa9c93adacf22fec4ef5ab48`;
- signer certificate SHA-256 `dd588fbb3093a047f81c24397fc6d2ab7f5a32040425a21dd24506cce59a9802`.

## Phone acceptance — NT8340A

Reference:
`NT8340A · 2006-04-18`

Accepted evidence:
- source files: `7653`;
- Runtime IR: `347` native sections;
- Fast Pack: `6138` files;
- outer package: `8012` files;
- deterministic package SHA-256:
  `7d6cc758b329adcd1ef46680de4d9c6815ff48b053f4e3550b06dd92b229b77e`;
- automatic validation/import PASS;
- existing NT8340A updated without duplicate;
- reopen exactly `NT8340A · 2006-04-18`;
- Modern shows `347 · native`;
- no compatibility fallback;
- Classic remains explicit alternate mode;
- post-completion stale replay regression PASS;
- active PREPARING rotation/background/external-app continuity PASS;
- PREPARING Cancel PASS;
- source unchanged and private staging cleaned;
- no new public `*_android` intermediate from the current flow.

Conclusion:
**v0.5.51 PHONE PASS — 2026-09-28.**

## Known non-blocking follow-ups

These are intentionally deferred until after the phone-accepted v0.5.51 runtime is merged:

- add a dismiss `×` for terminal COMPLETE/CANCELLED/FAILED status;
- fix `1 файлів` → `1 файл`;
- read-only audit of historical public `*_android` folders before any cleanup.

## Merge closeout

Before merge:
- final branch CI must be green after reconciliation with current `main`;
- PR #138 must be mergeable and ready for review;
- merge SHA must be written back to release metadata/handoff/ledger.
