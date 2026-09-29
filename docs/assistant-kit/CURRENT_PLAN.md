# Renault Docs — CURRENT PLAN

Останнє оновлення: 2026-09-29.

Це коротка жива точка відновлення. Історія рішень і старих інцидентів лишається в `CURRENT_HANDOFF.md` та `docs/assistant-kit/PROJECT_LEDGER.md`.

## Робочі правила

- Телефонний workflow — через `reno-docs` / меню Termux; ручні Git/gh/bash команди лише для аварійної діагностики або відсутньої функції меню.
- Після кожного завершеного кроку одразу оновлювати цей файл і довготривалий handoff/ledger.
- Не змінювати phone-accepted runtime v0.5.51 під час closeout. Новий UX — окремим follow-up після merge.
- Не видаляти legacy `*_android` без read-only provenance/reference audit.

## v0.5.51 — Kotlin-native raw Renault → .rdpkg

Статус: **PHONE PASS — 2026-09-28**.

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

## Closeout — public PR #1

Active repository:
`faric-ua/renault-docs-android`.

Private archive:
`faric-ua/renault-docs-android-private-archive` — history/reference only.

- [x] PHONE PASS recorded in release docs, handoff and ledger.
- [x] v0.5.51 branch transplanted into the active public repository without changing the phone-accepted Kotlin/Java runtime.
- [x] Public draft PR #1 created from `feat/v0.5.51-native-preparation-foundation`.
- [x] Public candidate version verified: `versionName 0.5.51`, `versionCode 67`.
- [x] Public Tests run `36582283061` — PASS.
- [ ] Public Android Debug run `36582283147` — waiting for completion.
- [ ] Mark public PR #1 ready and merge only after Android Debug is green.
- [ ] Record merged SHA / final release state in `RELEASE_META.json`, handoff and ledger.

### Wrong-branch build evidence — 2026-09-29

The phone-side menu build reported:

- repository: `faric-ua/renault-docs-android`;
- branch: `main`;
- commit: `97a5c6e29bd939197784f9ffced09161f5876bb2`;
- version: `0.5.50`;
- Android Debug run: `36580219193` — PASS.

This is a valid **main v0.5.50** build, but it is **not** the v0.5.51 closeout candidate. It exposed that the active phone clone had already moved to the new public repository while the v0.5.51 candidate still existed only in the private archive. That repository split is now reconciled by public PR #1.

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

**Перевірити завершення public Android Debug run `36582283147`. Якщо PASS — перевести public PR #1 з draft у ready, merge і записати merged SHA в release metadata / CURRENT_PLAN / CURRENT_HANDOFF / PROJECT_LEDGER. Додаткова дія на телефоні зараз не потрібна.**
