# v0.5.51 Regression Checklist

Status: **PASS**

Reference: `NT8340A · 2006-04-18`.

- [x] Existing project opens normally.
- [x] Existing NT8340A and NT8342A cards remain present.
- [x] Raw source selection requires one Renault volume, not a mixed parent folder.
- [x] Destination inside raw source tree is rejected.
- [x] Raw source is not modified.
- [x] Native preparation uses app-private staging.
- [x] No new public `*_android` intermediate is produced by this flow.
- [x] Rotation/background/external-app handoff keeps the same active run.
- [x] Stale Activity-result replay cannot start a second run after completion.
- [x] PREPARING Cancel stops safely and cleans private staging.
- [x] Generated `.rdpkg` validates and imports automatically.
- [x] Existing NT8340A is upserted without duplicate.
- [x] Reopen is `NT8340A · 2006-04-18`.
- [x] Modern is exactly `347 · native`.
- [x] No compatibility fallback.
- [x] Classic remains explicit alternate mode.
- [x] Package output is deterministic across successful reruns.

Known non-blocking follow-ups:
- terminal result/status dismiss `×`;
- singular Ukrainian wording `1 файл`;
- audit historical `*_android` folders before cleanup.
