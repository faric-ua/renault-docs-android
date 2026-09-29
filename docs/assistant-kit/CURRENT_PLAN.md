# Renault Docs — CURRENT PLAN

Останнє оновлення: 2026-09-29.

Це коротка жива точка відновлення. Історія рішень і старих інцидентів лишається в `CURRENT_HANDOFF.md` та `docs/assistant-kit/PROJECT_LEDGER.md`.

## Робочі правила

- Телефонний workflow — через `reno-docs` / меню Termux; ручні Git/gh/bash команди лише для аварійної діагностики або відсутньої функції меню.
- Menu item 19 — `Статус проєкту / build` — є read-only self-check: version/branch/commit, CURRENT_PLAN status/next step, release QA/delivery, latest Android Debug and Tests runs.
- Після кожного завершеного кроку одразу оновлювати цей файл і довготривалий handoff/ledger.
- Не змінювати phone-accepted runtime v0.5.51 під час closeout. Новий UX — окремим follow-up після merge.
- Не видаляти legacy `*_android` без read-only provenance/reference audit.

## v0.5.51 — Kotlin-native raw Renault → .rdpkg

Статус: **PHONE PASS + MERGED + DISTRIBUTION VERIFIED — CLOSED 2026-09-29**.

Reference:
`NT8340A · 2006-04-18`.

Accepted pipeline:

```text
raw Renault SAF folder
→ app-private normalized staging
→ Kotlin Modern / Section IR / Runtime IR
→ Fast Pack
→ manifests
→ streamed .rdpkg
→ validation/import
→ project upsert
→ native reopen
```

### Accepted phone evidence

- [x] Android/Kotlin raw-folder conversion only; Python/Termux не входять у production flow.
- [x] Source count: `7653` files.
- [x] Runtime IR: `347` native sections.
- [x] Fast Pack: `6138` files.
- [x] Outer package: `8012` files.
- [x] Deterministic package SHA-256 across successful reruns:
  `7d6cc758b329adcd1ef46680de4d9c6815ff48b053f4e3550b06dd92b229b77e`.
- [x] Automatic `RdpkgImporter.install()` validation/import PASS.
- [x] Existing NT8340A upserted without duplicate; project stayed at 2 volumes.
- [x] Reopen: `NT8340A · 2006-04-18`.
- [x] Modern: `347 · native`.
- [x] No compatibility fallback.
- [x] Classic remains explicit alternate mode.
- [x] Post-completion stale Activity-result replay regression fixed and phone-verified.
- [x] Active PREPARING lifecycle PASS: same copy run progressed `500/7653 → 1400/7653 → 2300/7653` through rotation/background/external-app handoffs.
- [x] PREPARING Cancel PASS: source unchanged; private staging cleaned.
- [x] Current Kotlin flow created no new public `*_android` intermediate.

### Accepted candidate / CI

Phone-accepted runtime source:
`6b334eafb1fbf9dcca591fa4d1b619885f32ea4b`.

CI for that candidate:
- Tests `36370544159` — PASS;
- Android Debug APK `36370544247` — PASS;
- artifact `Renault-Docs-v0.5.51-Debug`, id `10948508448`;
- APK SHA-256 `7b51c30cdd0be50de48ff8ddc21c6ee85f33c00eaa9c93adacf22fec4ef5ab48`;
- signer cert SHA-256 `dd588fbb3093a047f81c24397fc6d2ab7f5a32040425a21dd24506cce59a9802`.

## Closeout public PR #1

- [x] PHONE PASS recorded in release docs, handoff and ledger.
- [x] Reconcile PR branch with current `main` versions of shared Termux/signing infrastructure.
- [x] Resolve merge conflicts; PR is mergeable again.
- [x] Condense active plan and phone QA so stale PENDING chronology is not the primary source of truth.
- [x] Public Tests CI on final PR head — PASS.
- [x] Public Android Debug CI on final PR head — PASS.
- [x] Public PR #1 marked ready and squash-merged.
- [x] Merged SHA: `921e87f728a222e8f388a01ad989b082a7bd8894`.
- [x] Merge state recorded in release metadata.
- [x] Restore accepted development signer in public GitHub Actions Secrets.
- [x] Verify public-main signer continuity: SHA-1 `4102350e2787fd538bbf58a219293a132235e618`, accepted cert SHA-256 `dd588fbb3093a047f81c24397fc6d2ab7f5a32040425a21dd24506cce59a9802`.
- [x] Exact public-main APK SHA-256 `7b51c30cdd0be50de48ff8ddc21c6ee85f33c00eaa9c93adacf22fec4ef5ab48` matches the previously phone-accepted v0.5.51 APK byte-for-byte.
- [x] Final in-place install check PASS: exact public-main v0.5.51 installed over existing Renault Docs without uninstall/data reset; reopen kept Megane II with `Томів: 2`.

## Follow-up після merge

These are accepted work items, but they must not reopen the already accepted v0.5.51 runtime before merge:

1. **Terminal status dismiss UX**
   - right-side `×` for COMPLETE / CANCELLED / FAILED;
   - never during PREPARING / IMPORTING;
   - hides presentation state only;
   - does not delete package, volume, source or project data;
   - dismissal survives Activity recreation.

2. **Ukrainian wording**
   - `Імпортую .rdpkg… 1 файлів` → `Імпортую .rdpkg… 1 файл`.

3. **Legacy storage audit**
   Existing historical folders include:
   - `laguna 2 2001-2006_android`;
   - `Megane II_android`;
   - `Megane II_NT8342A_android`.

   Add a read-only Termux-menu audit: path, size, modified date, likely role/reference, then classify `KEEP / LEGACY / SAFE TO REMOVE`. Delete nothing automatically.

## Поточний наступний крок

**v0.5.51 CLOSED. Наступний окремий patch: terminal COMPLETE/CANCELLED/FAILED dismiss `×` + українське `1 файл` замість `1 файлів`. Після цього — read-only audit legacy `*_android` storage.**


Project separation:
- Renault Docs: continue only from this checkpoint.
- YTM importer: separate project; do not mix branches, menus, artifacts or QA evidence.


### 2026-09-29 signer restore helper

Merged helper:
`239663c4577c2e4d279a4052f1b0ac460b19bf9d`.

Renault Menu item `20 — Відновити accepted signer + build`:
- fetches the accepted signer only from the private archive at pinned ref;
- verifies keystore SHA-256 `7944e7d78bd2442021731a4cfd3105c06f475c13d70d4195b2378539604dfd10`;
- verifies certificate SHA-256 `dd588fbb3093a047f81c24397fc6d2ab7f5a32040425a21dd24506cce59a9802`;
- writes public GitHub Actions Secrets without printing secret values;
- then launches exact main Android build/download.

User action is now explicit and phone-side; no manual secret copy/paste is required.


### 2026-09-29 signing workflow hardening

Security hardening merged as:
`5aa665756e7b44de3dbb90bff2250d4bbe30596b`.

Android Debug signing workflow no longer runs automatically on pull requests.
Stable development signing secrets are now reachable only from:
- push to `main`;
- explicit `workflow_dispatch`.

PRs keep using the separate Tests workflow and cannot trigger the signing workflow automatically.

This closes the previously identified unnecessary PR secret exposure path.

User action remains:
`reno-docs → 5 → 20`.
Do not install the resulting APK until its signer/build output is reviewed.


### 2026-09-29 pre-signer-update phone baseline

Phone screenshot before the signer-continuity install check confirms:
- currently installed Renault Docs reports `v0.5.51`;
- Megane II project is present with `2` volumes;
- Laguna II and Kangoo II placeholder projects remain present;
- app data is intact before attempting any new public-main APK update.

This screenshot is a baseline only; it does not identify the APK signer by itself.

Next user action remains:
`reno-docs → 5 → 20`, then return the complete restore/build output before installing the generated APK.


### 2026-09-29 signer continuity verified

Exact trusted public-main build:
- commit `fc7ffc6a7deda7074e5d6b2e631380fd923401c0`;
- Android Debug run `36603132862` — PASS;
- artifact id `11050306597`;
- signer SHA-1 `4102350e2787fd538bbf58a219293a132235e618`;
- accepted signer SHA-256 verified by workflow: `dd588fbb3093a047f81c24397fc6d2ab7f5a32040425a21dd24506cce59a9802`;
- APK SHA-256 `7b51c30cdd0be50de48ff8ddc21c6ee85f33c00eaa9c93adacf22fec4ef5ab48`;
- artifact ZIP SHA-256 `ddf9eb0ee9ef53e31ce997d157855c6bb3b1d7111cc1e662686039ddc4f55bb2`.

Important:
the public-main APK SHA-256 is exactly the same as the already phone-accepted v0.5.51 candidate APK. Signing continuity and APK identity are therefore cryptographically confirmed. Only the final Android in-place install/data-preservation check remains.


### 2026-09-29 final in-place install PASS

Post-install phone screenshot confirms the exact public-main v0.5.51 update preserved application state:
- app opens as `v0.5.51`;
- `Megane II` remains present;
- `Томів: 2` remains present;
- Laguna II and Kangoo II placeholder projects remain present.

No uninstall/data reset was used for this acceptance check.

Final v0.5.51 distribution verdict:
**READY / CLOSED**.

All v0.5.51 gates are now closed:
runtime PHONE PASS, merge, signer continuity, trusted public-main build, exact APK identity, in-place update, and data preservation.


## v0.5.52 — terminal status polish

Status: **READY / CLOSED — 2026-09-30**.

Phone-accepted source:
`8a364ef9e50d0bd81273e9d419321284abdfed03`.

Merged public main:
`31c26e9c24abab6fb491f8aa9290c5395112a2ca`.

Accepted:
- [x] terminal COMPLETE / CANCELLED / FAILED status has right-side `×`;
- [x] active PREPARING / IMPORTING never shows terminal dismiss `×`;
- [x] dismissal survives rotation and ProjectActivity reopen;
- [x] dismissal is presentation-only;
- [x] Ukrainian file-count wording fixed;
- [x] open native preparation dialog live-updates progress every 750 ms;
- [x] candidate Tests `36639849368` PASS;
- [x] candidate Android PR Check `36639849222` PASS;
- [x] trusted candidate build `36640408798` PASS;
- [x] phone acceptance PASS, including live progress;
- [x] PR #6 squash-merged;
- [x] public-main Tests `36642605889` PASS;
- [x] public-main Android Debug `36642605895` PASS;
- [x] public-main artifact id `11067202339`;
- [x] stable signer SHA-1 `4102350e2787fd538bbf58a219293a132235e618`;
- [x] public-main APK SHA-256 `2a2fc9c952ac24c11b3022786b1f6e1eccd98c5a595532317f15f080d01797aa`;
- [x] public-main APK is byte-for-byte identical to the already installed and phone-accepted final candidate, so a redundant reinstall adds no new binary evidence.

## Поточний наступний крок

**v0.5.52 CLOSED. Start the read-only provenance/reference audit of historical `Documents/Renault/*_android` folders. Delete nothing automatically.**

