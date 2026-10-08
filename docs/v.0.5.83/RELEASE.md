# Renault Docs v0.5.83 / build 99 — Home New Volume handoff

Status: **PR #89 MERGED / MAIN TESTS PASS / STABLE-SIGNED APK READY / CORE ROUTE PHONE PASS / EXTENDED QA PAUSED**.\n\nSource SHA: `b5f1c22d73642d7341943e4320b5fb469cbe9bec`. PR Tests #573 PASS / Android PR Check #457 PASS; main Tests #574 PASS / signed Android Debug APK #146 PASS. Artifact `Renault-Docs-v0.5.83-Debug` ID `11582942367`, GitHub artifact digest `sha256:b65dd8100cb4412c7a79933ad2c12bc4456859f37ff1a4268298933f3e01d1ef`, expires 2026-10-11 22:27:09 UTC. This is an **artifact digest** and must not be mislabeled as the unpacked APK checksum. Do not install/uninstall without user choice; existing data must be retained.

## Scope

Fix #79 without touching imported data or changing layout outside the navigation contract:

- Home `Додати` → `Новий том` opens `ProjectChooserActivity`.
- Tapping a project now opens `ProjectActivity` with `showAddPanel = true`; the project `Додати` body is expanded explicitly without opening SAF automatically.
- The user now chooses `Авто` (prepared .rdpkg), `Вручну` (folder), `Створити .rdpkg з raw`, or `Створити .rdpkg з архіву` via the existing buttons.
- The `openPicker = true` route remains intact for intentional future/other direct-picker callers.
- First entrance expansion preserves persisted portrait pin preference and is restored across rotation; landscape initial handoff can show choices without switching persisted pin.
- No import, conversion, file movement or removal happens solely on selecting a project.

## Safety and release gates

- Automated route contract `tests/test_v0583_home_new_volume_route_contract.py` and pre-existing Python tests.
- Android PR Check / main signed APK must pass before offering install.
- User reported «Пасс» on 2026-10-09 for the core New Volume route after the signed build became available. **Core phone acceptance PASS**; Back/rotation/individual Auto/Manual/raw/archive actions remain untested and extended QA is paused.
- Existing source ZIP, .rdpkg and registered Renault volumes remain untouched.
