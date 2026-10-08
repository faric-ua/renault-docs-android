## v0.5.82 / build98 — CORE PHONE PASS / EXTENDED QA NEXT — 2026-10-08

- [x] Home Add layout, pin/expand parity, scroll split, tools/ready projects + explanatory text + legacy tiles moved.
- [x] Global landscape Activity and app-owned AlertDialog status/navigation bar hiding; portrait restoration; IME/SAF untouched, PDF fullscreen preserved.
- [x] PR #88 merged; runtime SHA `f17b319c4e17d8f7005ae20221dd7bd642cb172b`.
- [x] PR Tests #571 PASS / Android PR Check #456 PASS; main Tests #572 PASS / signed APK #145 PASS. Artifact `Renault-Docs-v0.5.82-Debug` ID 11563839781, sha256 `94cadb5caf5b1af93a2a959e674819c31f0571b479c66f7b9a492af6ae6586be`.
- [x] Installed/tested on phone; user core acceptance «Пасс. Взагалі все чудово.» 2026-10-08. Do not uninstall/clear data or move/delete archives.
- [x] Core phone QA: Home Add/My Renault, 📌/expand and portrait→landscape→portrait system-bar hide/restore — PASS.
- [ ] NEXT extended checks: Add contents, Ready Projects/Drive, Tools/Legacy, independent scroll/status, Help/Dialog, Viewer/Settings landscape and SAF/IME unaffected.
- [ ] Only after phone evidence: mark accepted/close; do not treat v0.5.81 Viewer pending check as PASS.

Resume `docs/assistant-kit/SESSION_CHECKPOINT_2026-10-08_v0.5.82.md` and `docs/v.0.5.82/qa/PHONE_TEST.md`.

---

## v0.5.81 / build97 — MAIN CI PASS / SIGNED APK READY / PHONE QA NEXT — 2026-10-08

- [x] Audit 20 app-owned AlertDialogs and unify all via DialogUi (Viewer 2 fixed).
- [x] Compact archive preflight, expandable full SAF/source/duplicate details, rotation state.
- [x] PR #87 MERGED; app SHA `309f068bbf92c65ca06c0cc2e38887d6a4664837`.
- [x] Main Tests #568 PASS; Android Debug APK #144 PASS; signed artifact 11560786817, sha256 `5711b9b6181edd4edaa53a575bc23fa13186a68aea925544de71b2b78b1afe79`.
- [x] v0.5.81 dialog test performed on phone; install-over method not independently documented. Never uninstall/clear data.
- [x] User PHONE PASS: expanded technical details survive rotation; Cancel starts no conversion (2026-10-08). Compact dialog acknowledged; detailed source diagnostic preservation not separately checked.
- [ ] NEXT: audit Viewer section navigator first; then Home/Settings/Help, keyboard under modal and long content. Do not use destructive actions.
- [ ] Closeout only after user phone evidence; keep v0.5.80 pending subtests distinct.

Resume: `docs/assistant-kit/SESSION_CHECKPOINT_2026-10-08_v0.5.81.md` and `docs/v.0.5.81/qa/PHONE_TEST.md`. No automatic file moves/deletions.

---

## v0.5.80 / build96 — MAIN CI PASS / PHONE QA NEXT — 2026-10-08

Read `docs/assistant-kit/SESSION_CHECKPOINT_2026-10-08_v0.5.80.md` first. PR #86 merged. Runtime source `5a6ddc060edb4ff452eb1e3e96c1423cc042ddf9`. Main Tests #563 PASS, Android Debug APK #143 PASS; signed artifact `Renault-Docs-v0.5.80-Debug` ID `11557433214` (sha256 `0da82672e308148e5a2178f9fb147705b5ab8f2b96c39da15220e09dde6223b6`). Phone QA PENDING. Preserve source ZIP/.rdpkg and installed volumes; no migration/cleanup. Next: install via Renault Menu 5 → 19 → 8 → 13, test source guard, strong model mismatch, SAF source confirmation, global registered-project NT preview, Cancel/rotation. Issues #85/#82 candidate not closed, #83 picker history unresolved, #81 source preservation, #79 Home picker separate.

---

## CURRENT — actual source provenance captured from phone / fix scope next

- v0.5.79 read-only diagnostic proves source was `Kangoo-II_X61_NT8486...rdpkg` at `Documents/Renault/packages/rdpkg/`, source kind ARCHIVE_FILE, target Megane II, output `megane-ii-nt8486-2009-08-31`.
- Start 2026-10-08 15:13:32, finish 15:15:50, output SHA256 `4ed38ab4a3ea8dcf41c24cf86d25820d820f896652062b682eab7f1ef0de53f2`.
- Code confirms `.rdpkg` can appear in archive picker (generic application/octet-stream) and be treated as ZIP by magic signature; selected project's model used for output.
- No archive-source deletion evidenced. Exact picker browsing history unknown despite user asserting Megane Sources.
- Issues: #85 block .rdpkg + selection preflight; #82 model mismatch; #83 unclear picker selection history; #81 immutable original; #79 Home chooser.
- Next: agree scoped safe patch; do not delete files or trigger destructive reimport. Progress UI v0.5.78 phone QA separate.

---

## CURRENT — collect read-only last-source report v0.5.79 / build95

- Main signed APK #142 PASS / Tests #561 PASS / app SHA `b6fdfa1efc90f0c4db9be2fc30898ddb8dfac58a`.
- Diagnostic artifact ID `11553367269` (stable signer).
- Install-over only through Renault Menu `5 → 19 → 8 → 13`, no uninstall/clear or new archive conversion.
- Project Megane II → expand Add → diagnostic → Copy report → paste into chat.
- Model/source provenance issue #83 unresolved. #82 model prevention, #81 immutable original, #79 Home picker separate.

---

## CURRENT — v0.5.79 source provenance read-only

Implement isolated diagnostic candidate, CI → signed APK → install over existing → Project Add copy report. No new native .rdpkg before evidence capture. User confirms none since NT8486; do not assert exact source yet. Issue #83 root cause unknown; #82 model mismatch followup, #81 source immutability, #79 Home chooser.

---

## TOP PRIORITY — issue #83 preserve last native archive source record

1. No new phone native conversions or clearing cache/data: `NativeRdpkgRunStore` persists sourceUri/sourceName/projectId after completed operation; next `clearFinished()` erases record.
2. Read-only capture of persisted origin for Megane-II_X61_NT8486 (actual Kangoo II X61 PDF content despite user selecting file in Megane Sources).
3. Diagnose source selection URI, SAF picker, resumed/batch state and mismatched archive; no blame/guess.
4. Issue #82 prevent packaging conflicting source model; #81 source immutable. v0.5.78 phone QA suspended for archive actions.
5. Preserve deployed app/data; no extra builds or mutations without explicit next step.

---

## CURRENT — investigate issue #81 source archive disappearance

- Stop archive-related phone QA until original file location and status are understood.
- Data safety audit: main v0.5.78/build 94, archive intake read-only via SAF and app-private copy. Cleanup deletes staging or generated output, not intentionally source; no complete proof of nondeletion for all providers.
- Ask exact original filename, source provider/path and destination. Read-only search (My Files, Downloads, Trash, upstream PC/Drive) first. Test using disposable archive only.
- Track issue #81 as OPEN; avoid speculative fixes and avoid rebuilding unrelated release.
- v0.5.78 phone QA otherwise pending. Issue #79 and Home panel redesign remain later.

---

## CURRENT — v0.5.78 / build 94 — phone QA next

- PR #80 merged, main app SHA `948d5688646728d53b47327cb56f222d316b6ca5`.
- Main Tests #557 PASS / Android Debug APK #141 PASS / artifact id `11548219781`.
- Next: install via Renault Menu 5 → 19 → 8 → 13; perform phone QA for status inside volume Add.
- Main/Home project panel adaptation is next separate stage, **not yet done**.
- Home «Новий том» ready-.rdpkg auto-picker route confirmed, issue #79 **OPEN**.
- Do not assume phone PASS; preserve v0.5.77 accepted pin/orientation behavior.

---

## ACTIVE — v0.5.78 compact inline progress for volumes

Feature branch: `feat/v0.5.78-inline-volume-operation-status`, target v0.5.78/build 94. PR CI next; only after PASS main APK then phone QA. Keep accepted v0.5.77 pin/orientation unchanged. Home panel reuse and issue #79 later.

---

## NEXT CHAT RESUME — 2026-10-08

Full checkpoint: `docs/assistant-kit/SESSION_CHECKPOINT_2026-10-08_v0.5.77.md`. v0.5.77 / build 93 PHONE PASS 6/6 / CLOSED.
Canonical next task: discuss status window/panel UX; **do not start code changes without new requirements**.
Live plan: `docs/assistant-kit/CURRENT_PLAN.md`.

---

## CURRENT — v0.5.77 CLOSED / status-card review next — 2026-10-08

- Phone QA v0.5.77 / build 93: PASS 6/6; comment «Все норм».
- Portrait↔landscape pin/collapse/scroll/persistence accepted; no further runtime action for v0.5.77.
- Main CI: Tests #555 PASS / Android Debug APK #140 PASS.
- Next product discussion: status-window/status-card layout, previously deferred. Do not implement its redesign without requirements or review.
- Separate issue #68 Home action-panel follow-up remains pending; don't close based solely on Project-screen acceptance.

---

## ACTIVE — v0.5.77 phone rotation QA — 2026-10-08

- v0.5.76 build 92: PHONE PASS 7/7.
- PR #77 MERGED into main as `87ab5c2dc20fa14dd7e88ac8cfb81d746c262169`.
- Main Tests #555 PASS; Android Debug APK #140 PASS; artifact `Renault-Docs-v0.5.77-Debug` id `11523858486`.
- v0.5.77/build 93: landscape Add auto-collapses and grayscale pin becomes inactive; manual expansion in landscape available; portrait pinned/expanded state is preserved.
- After signed APK ready: Renault Menu `5 → 19 → 8 → 13`; test rotation and scroll. Do not change status card in this release.

---

## ACTIVE — v0.5.76 original emoji pin — 2026-10-08

- Restore original Android 📌 emoji glyph exactly, not rotated flat vector.
- Pinned: normal full color. Unpinned: grayscale render of same glyph.
- Preserve v0.5.75 sticky Add and status-card layout.
- PR #76 MERGED; Tests #551 PASS; Android Debug APK #139 PASS; artifact id `11523497323`.
- Next: phone QA through Renault Menu `5 → 19 → 8 → 13`. No additional runtime changes before visual acceptance.

---

## ACTIVE — v0.5.75 sticky Add phone QA — 2026-10-08

- runtime: `b25efbee254562d7b3e9f6917772e8d2e6370eb3`
- v0.5.75 / build 91
- Tests #547 PASS
- Android Debug APK #138 PASS

Current action:
`5 → 19 → 8 → 13` → install → verify sticky Add, list scrolling, spacing, and pin colors.

---

## ACTIVE — v0.5.75 sticky Add panel — 2026-10-08

Implementation:
- Add outside volume ScrollView;
- volumes scroll independently;
- 10dp gap before first volume;
- diagonal pin restored;
- inactive pin light / active pin red;
- status-card review deferred.

Next gate:
CI → merge → phone QA.

---

## ACTIVE — v0.5.74 Add panel phone QA — 2026-10-08

- runtime: `8dc7a3d0c6d22c50df343a36456f6bd4716b3a5a`
- version: v0.5.74 / build 90
- Tests #542 PASS
- Android Debug APK #137 PASS

Current action:
`5 → 19 → 8 → 13` → install → open Megane II → verify visual/status refinements.

---

## ACTIVE — v0.5.74 Add panel status refinement — 2026-10-08

Current implementation:
- transient status card inside expanded Add;
- hidden when blank;
- guidance removed;
- vector pin: neutral inactive / red pinned;
- heavier ▲ / ▼;
- progress remains outside.

Next gate:
CI → merge → phone QA.

---

## ACTIVE — v0.5.73 Add panel phone QA — 2026-10-08

- runtime: `871d93c6f0416f9b98ea78edbab06a9ddcce8eba`
- v0.5.73 / build 89
- Tests #538 PASS
- Android Debug APK #136 PASS
- issue #68 implementation merged

Current user action:
`5 → 19 → 8 → 13` → install → open Megane II.

First visual gate:
only one compact full-width `Додати` header should be visible above the volume list.

---

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

