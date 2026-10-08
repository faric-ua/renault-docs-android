# Renault Docs v0.5.83 / build 99 — Home New Volume handoff

Status: **PR #89 MERGED / MAIN TESTS PASS / STABLE-SIGNED APK READY / CORE ROUTE PHONE PASS / EXTENDED QA PAUSED**.\n\nSource SHA: `b5f1c22d73642d7341943e4320b5fb469cbe9bec`. PR Tests #573 PASS / Android PR Check #457 PASS; main Tests #574 PASS / signed Android Debug APK #146 PASS. Artifact `Renault-Docs-v0.5.83-Debug` ID `11582942367`, GitHub artifact digest `sha256:b65dd8100cb4412c7a79933ad2c12bc4456859f37ff1a4268298933f3e01d1ef`, expires 2026-10-11 22:27:09 UTC. This is an **artifact digest** and must not be mislabeled as the unpacked APK checksum. Do not install/uninstall without user choice; existing data must be retained.

## Durable GitHub Release (published 2026-10-09)

**GitHub prerelease published and assets verified:** [v0.5.83-debug](https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.83-debug)

- PR #90 added an **explicit**, version-reviewed release promotion workflow (not automatic on every Android build); publication run [#37860223840](https://github.com/faric-ua/renault-docs-android/actions/runs/37860223840) PASS.
- Original stable-signed APK: `Renault-Docs-v0.5.83-debug.apk`, SHA-256 `8592b1d8aacdc4db8b856d899128d7a84931fad7b69276b3baea7eeadf4f3de9`.
- Separate `.apk.sha256` release asset included. **This digest is the actual APK**, unlike the Actions bundle digest noted above.
- PR #91 added Termux menu 8 fallback: if the Actions artifact has expired, download this Release only if source/version/Android code are compatible, then verify `.sha256` before and after copying. Python/shell CI PASS; no new Android build.
- Public **debug prerelease** remains downloadable after the 3-day Actions artifact expires, unless the repository owner removes the release. No expiry for installed app.

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
