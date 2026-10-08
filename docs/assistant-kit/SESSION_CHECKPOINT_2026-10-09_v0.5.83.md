# Renault Docs — handoff v0.5.83 / build 99 — 2026-10-09

## Confirmed delivery state

**PR #89 MERGED / PYTHON+ANDROID MAIN CI PASS / STABLE-SIGNED APK READY / PHONE QA PAUSED (NOT CLOSED)**.

- PR: https://github.com/faric-ua/renault-docs-android/pull/89; issue #79 remains open until on-phone acceptance.
- Application source/merge SHA: `b5f1c22d73642d7341943e4320b5fb469cbe9bec`.
- PR Tests #573 PASS, Android PR Check #457 PASS.
- Main Tests #574 PASS, Android Debug APK #146 PASS with stable dev signer.
- Artifact `Renault-Docs-v0.5.83-Debug`, ID `11582942367`, GitHub artifact bundle digest `sha256:b65dd8100cb4412c7a79933ad2c12bc4456859f37ff1a4268298933f3e01d1ef`, expiry `2026-10-11 22:27:09 UTC`. GitHub artifact digest ≠ guaranteed raw APK checksum.
- GitHub Actions: https://github.com/faric-ua/renault-docs-android/actions/runs/37853352879.
- If user later wants: install **over existing** from Renault Termux `5 → 19 → 8 → 13`; never uninstall/clear app data or delete/move source ZIP, RDPKG, installed volumes.

## Exactly what changed (issue #79)

- Previous: Home `Новий том` → chooser → ProjectActivity with `openPicker = true` → immediately `openPackagePicker()` (prepared RDPKG only).
- Now: Home `Новий том` → chooser → ProjectActivity with `showAddPanel = true`. On new launch, project `Додати` opens expanded with **Авто / Вручну / Створити .rdpkg з raw / Створити .rdpkg з архіву**; no SAF, conversion or import starts automatically. The existing intentional `openPicker` option remains supported separately.
- Expansion respects saved portrait pin; initial handoff also visible in landscape; savedInstanceState takes precedence on rotation.
- Changes limited to ProjectActivity.kt, ProjectChooserActivity.kt, version, release contracts/tests and docs. No project data migration.
- New contract `tests/test_v0583_home_new_volume_route_contract.py`; see `docs/v.0.5.83/RELEASE.md`.

## Phone QA is intentionally paused

User requested to stop the manual-test sequence after an earlier v0.5.82 video; do not ask for new tests/reinstallation unprompted. v0.5.83 code/tests/CI PASS but has **NO direct device evidence**. If user resumes: very short test only of New Volume → select Megane II → Add options visible, no picker; Back/rotation safe without import. Full acceptance cannot be claimed yet.

## Unfinished unrelated functionality or QA

- #40 background lifecycle across long-running operations (audit incomplete).
- #51 full archive intake acceptance (supported paths implemented, extensive QA paused).
- #30 Windows original archive → validated prepared catalog publication pipeline (separate track).
- Optional #21 support/donate URL not selected; #50 Termux item 19 simplified presentation.
- Historical #85/#82 preflight and model guards implemented and phone-observed on v0.5.80; issues not reconciled/closed. #81/#83 provenance/immutability investigations not grounds for cleanup.
- v0.5.82 Test 8 video `607185.mp4` PASS for Home/Project Add collapse and visibility. Test 7 status N/A, no active run.
- Preserve authentic Renault Classic; keep Modern responsive active-category presentation in landscape.

## Next user choice

No automatic further actions. Once user chooses, either resume targeted phone acceptance of v0.5.83 or implement another scoped remaining feature. Never perform background work, data cleanup or unrelated conversions without explicit request.
