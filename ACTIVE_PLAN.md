## ACTIVE — v0.5.73 compact Add panel — 2026-10-08

v0.5.72 duplicate fast-path: **PHONE PASS**.

Current code work:
- issue #68;
- version v0.5.73 / build 89;
- implement one collapsible/pinnable `Додати` panel;
- preserve all existing add/create flows;
- keep operation status outside the collapsible body.

Next gate:
CI, merge, then phone visual/lifecycle QA.

---

## ACTIVE — v0.5.72 duplicate ZIP phone re-test — 2026-10-07

- runtime: `eacbbc1f914d4f35564724501a3edb126fd71963`
- v0.5.72 / build 88
- Tests #535 PASS
- Android Debug APK #135 PASS
- artifact `Renault-Docs-v0.5.72-Debug`

Current user action:
`5 → 19 → 8 → 13` → install → repeat exact NT8266A duplicate ZIP.

PASS requires **no `Розпаковую ZIP...` stage**.

After PASS:
Issue #68 — compact collapsible/pinnable `Додати` panel.

---

## ACTIVE — v0.5.71 duplicate ZIP phone re-test — 2026-10-07

- runtime source: `131b4dcef2ed95e35198a8cb03a0ce4322fda64c`
- version: `0.5.71` / build `87`
- Tests #532: PASS
- Android Debug APK #134: PASS

Current action:
`5 → 19 → 8 → 13` → install → repeat exact NT8266A duplicate ZIP.

Expected:
- copy/inspection may appear;
- `Розпаковую ZIP...` must NOT appear;
- terminal `Том уже є`;
- Megane II stays at 9 volumes.

Next after this gate: collapsible/pinnable `Додати` panel.

---

## ACTIVE — v0.5.70 duplicate ZIP phone re-test — 2026-10-07

- runtime source: `b9c763237b7aceede742c6aa559f53abf4c83444`
- version: `0.5.70` / build `86`
- Tests #530: PASS
- Android Debug APK #133: PASS
- artifact: `Renault-Docs-v0.5.70-Debug`

**Current next action:**
`5 → 19 → 8 → 13` → install over current Renault Docs → repeat the exact same NT8266A duplicate ZIP.

Expected phone evidence:
- `Том уже є`;
- no `Розпаковую ZIP...` progress card;
- Megane II remains 9 volumes.

---

## ACTIVE — v0.5.69 Archive Intake phone gate — 2026-10-07

**Code is merged. Do not continue old implementation TODOs.**

- main: `53f4208b4b2e4d27e4bb55094c855eaf13fb2442`
- version: `0.5.69` / build `85`
- PR #53: MERGED
- Tests #526: PASS
- Android Debug APK #132: PASS
- issue #51 stays open until phone acceptance

Current next action:
`Renault Menu 5 → 19 → 8 → 13` → install over existing app → run first real **ZIP** Archive Intake test.

Phone QA order:
1. install/startup/data preservation;
2. single-volume ZIP;
3. exact duplicate ZIP;
4. multi-volume ZIP chooser;
5. rotation + background + screen lock;
6. Cancel and cleanup;
7. verify source archive unchanged and selected/new volumes installed exactly once.

---

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

## PHONE QA CHECKPOINT — NT8266A duplicate ZIP — 2026-10-07

Status: **duplicate prevention PASS / full v0.5.69 acceptance still in progress**.

Observed on phone:
- Megane II archive flow processed an archive that resolved to `NT8266A · 2004-06-28`;
- final terminal result: `Том уже є`;
- conversion was skipped;
- no claim yet for the entire release gate.

Performance finding:
- this archive still reached visible ZIP extraction (`3732 / 4378` files) before duplicate identity was proven;
- tracked as `PERF-ARCHIVE-001`;
- functional behavior is correct, early duplicate fast-path needs future optimization.

**Current next action:**
1. close the terminal status;
2. confirm Megane II still shows **9 volumes**;
3. then test a **single-volume ZIP whose volume is NOT installed yet**;
4. require one canonical .rdpkg + one new installed volume, exactly once;
5. verify the original ZIP remains unchanged.

After that: multi-volume chooser → rotation → background/lock → Cancel/cleanup.

