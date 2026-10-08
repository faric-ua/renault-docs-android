# Renault Docs v0.5.83 / build 99 — Home New Volume handoff

Status: **CANDIDATE / CI PENDING / PHONE QA PAUSED PER USER**.

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
- User has paused extended manual phone QA; **do not mark this version phone-accepted without a new user report**.
- Existing source ZIP, .rdpkg and registered Renault volumes remain untouched.
