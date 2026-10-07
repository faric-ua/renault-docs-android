# Renault Docs — ACTIVE PLAN

Updated: 2026-09-29

The canonical live crash-recovery checklist is:

`docs/assistant-kit/CURRENT_PLAN.md`

Do **not** maintain a second independent checklist in this root file.

Current status:
- v0.5.51 raw Renault → Kotlin-native `.rdpkg` is **PHONE PASS**;
- PR #138 is reconciled with current `main` and mergeable;
- closeout is waiting for final CI / merge bookkeeping;
- terminal-status dismiss UX, `1 файл` wording, and legacy `*_android` audit are post-merge follow-ups.

Resume rule:
1. read `docs/assistant-kit/CURRENT_PLAN.md`;
2. read `CURRENT_HANDOFF.md` only for broader history/context;
3. continue from the single **Поточний наступний крок** in CURRENT_PLAN.

This file exists only as a stable root-level pointer for older tooling and context instructions.


## ACTIVE — Archive Intake after v0.5.68 — 2026-10-07

**Baseline:** v0.5.68 / build 84, main `41e6801b985a344922169b8e8e2b60b535f7b60e`, Tests #481 PASS, signed APK #131 PASS.

**Now:** implement issue #51, target v0.5.69 / build 85.

Desired flow:
`archive file (ZIP/7Z/RAR) → private safe staging → detect Renault raw root(s) → existing native raw→.rdpkg → validate/install → cleanup staging`.

Rules:
- never mutate the source archive;
- do not duplicate the package builder;
- no writes outside app-private staging and explicit destination;
- prevent archive path traversal;
- multiple detected volumes require explicit user selection;
- preserve foreground notification/background/screen-lock behavior;
- keep opaque Renault IDs unchanged.


### Release gate — Termux menu

Every phone candidate for this stage must be delivered through Renault Menu:
`5 → 19 → 8 → 13`.

Do not tell the user to use item 7 as the normal update path. Item 7 is only for deliberately triggering a new build when the exact current commit has no successful APK artifact.


## ACTIVE CHECKPOINT — v0.5.69 Archive Intake — 2026-10-07

Resume from draft PR **#53**, branch `feat/v0.5.69-archive-intake`, head `8de99f551ad56e6ec61b9a2372514217dc08cb7c`.

CI at checkpoint:
- Tests #497 PASS
- Android PR Check #395 PASS

Next exact work order:
1. sync feature branch with current main `b8cd2047b93b8ef6c8c4e160a9848c30ead113b0`;
2. implement multi-root chooser with already-installed marks/default deselection;
3. add pre-extraction duplicate fast path where archive metadata makes identity safe;
4. finish rotation/background/cancel/cleanup contracts;
5. rerun CI;
6. phone candidate via **5 → 19 → 8 → 13** only.

Already implemented duplicate rule: exact installed volume is detected before `engine.prepareLocal(...)`, conversion is skipped, and UI ends with `Том уже є` rather than wasting time generating/importing the same volume.
