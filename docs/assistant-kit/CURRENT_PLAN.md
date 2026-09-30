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

## Legacy `*_android` storage audit

Status: **CLOSED — 2026-09-30**.

Final real-phone result:
- `Megane II_android` migrated/quarantined/reopen-tested and permanently removed;
- `Megane II_NT8342A_android` migrated/quarantined/reopen-tested and permanently removed;
- both NT8340A and NT8342A remained functional while both legacy folders were absent;
- final read-only audit shows only `laguna 2 2001-2006_android`;
- Laguna remains **KEEP** as the configured active `build_root`;
- final audit summary: `KEEP 1 / LEGACY 0 / SAFE TO REMOVE 0`;
- approximately 805 MB of obsolete Megane prepared data was removed.

## Dataset link integrity gate

Status: **REAL-PHONE BASELINE FAIL / SOURCE-BUILD PARITY DIAGNOSIS IMPLEMENTED / CI PENDING**.

Scope:
- [x] read-only scanner for prepared dataset HTML/CSS local references;
- [x] checks HTML `href/src/background/action/data/poster`;
- [x] checks CSS `url(...)` and quoted `@import`;
- [x] strips query/fragment before filesystem resolution;
- [x] ignores external/data/javascript/mail/tel references;
- [x] validates manifest path fields such as entrypoint/modern/runtime/fast-pack references;
- [x] JSON report with source/reference/resolved path;
- [x] nonzero exit code when missing local targets are found;
- [x] Termux Menu item `22 — Перевірити посилання dataset (read-only)`;
- [x] unit tests for valid, missing, external, fragment/query and outside-root references;
- [x] checker does not mutate the dataset.

## Laguna DATA-001 repair

Status: **CLOSED / REAL-PHONE PASS — 2026-09-30**.

Final evidence:
- restored seven prepared Classic volume folders from `_volumes_hold`;
- source volumes: 10;
- build volumes: 10;
- missing volumes: 0;
- extra volumes: 0;
- HTML/CSS files scanned: 48307;
- local references checked: 264779;
- missing local targets: 0;
- volume parity: PASS;
- dataset link integrity: PASS.

This confirms there is no remaining link breakage from the user's earlier language cleanup.

## Laguna II batch .rdpkg preparation

Status: **MERGED / CI PASS / PHONE BATCH BUILD PENDING**.

Target:
- full Laguna dataset contains 10 validated volumes;
- package model remains `1 volume = 1 .rdpkg`;
- do not create one monolithic 10-volume package.

Implementation:
- existing menu item 15 remains the package entrypoint;
- when a dataset has multiple volumes, it now offers `A — Усі томи окремими .rdpkg`;
- batch iterates every discovered packageable volume;
- each volume uses the existing single-volume `build_rdpkg()` pipeline and its validation;
- output remains under `Documents/Renault/packages/rdpkg`;
- batch writes one JSON summary containing package paths, identities, SHA-256, byte sizes and payload file counts;
- single-volume selection remains available unchanged.

## Поточний наступний крок

**PR #11 merged as `3c5a7d6d75ef3cb357b3d11602977678dd66dabd`; Tests `36656945889` PASS. On phone: menu 5 → menu 15 → select Laguna dataset → A — all volumes separately. Capture the final READY · RDPKG BATCH summary.**
