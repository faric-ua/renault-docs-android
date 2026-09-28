# Renault Docs v0.2.0 — Converter foundation

## Goal

Додати перший реальний Android flow для майбутньої конвертації старої Renault-папки без Termux/Python.

## Scope

- enable `Конвертувати стару папку`;
- окремий converter screen;
- source folder через SAF;
- destination folder через SAF;
- persistable URI permissions;
- draft state, який переживає Activity recreation без повторного відкриття picker;
- same-source/destination guard;
- Kotlin port базової path-normalization логіки з unit tests;
- жодного `MANAGE_EXTERNAL_STORAGE`.

## Deliberately not in this release

v0.2.0 ще не запускає масове копіювання 60k+ файлів і не створює готовий dataset. Це окрема наступна operation wave після перевірки SAF/lifecycle foundation на реальному телефоні.

## Evidence rule

CI PASS не є PHONE PASS. Rotation, picker cancel/return і Back мають бути перевірені на телефоні.
