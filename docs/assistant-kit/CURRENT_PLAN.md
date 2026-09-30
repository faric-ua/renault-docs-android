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

Status: **REAL-PHONE READ-ONLY AUDIT PASS / MIGRATION BEFORE CLEANUP**.

Real phone result on 2026-09-30:
- `laguna 2 2001-2006_android` — 356M / 19836 files / dataset manifest present / **KEEP** because it is the configured active `build_root`;
- `Megane II_NT8342A_android` — 405M / 7855 files / dataset manifest + `.nomedia` / **KEEP FOR NOW** because this prepared folder is the historical source of the current NT8342A project volume;
- `Megane II_android` — 400M / 7664 files / dataset manifest + `.nomedia` / legacy single-volume Megane output from v0.5.37, historically used for NT8340A; current NT8340A has since been replaced by an app-private `.rdpkg` installation, so this is a **SAFE-TO-REMOVE CANDIDATE AFTER FINAL REFERENCE GATE**.

Important code fact:
`.rdpkg` imports are extracted into app-private `noBackupFilesDir/rdpkg/<packageId>` and project volume records then use the app-owned DocumentsProvider tree URI. They do not depend on the public `*_android` folder after a successful upsert.

Menu presentation bug found during this audit:
item 21 accidentally contained a literal `\n`; fixed and merged via PR #8 as `3d056d5641b8351163a844af816b43c7ec0b6ef7`.

## Поточний наступний крок

**Both Megane quarantine gates PASS: with both `Megane II_android` and `Megane II_NT8342A_android` renamed out of place, NT8340A and NT8342A both reopen successfully. Both quarantined Megane folders are now SAFE TO REMOVE. Keep `laguna 2 2001-2006_android`.**
