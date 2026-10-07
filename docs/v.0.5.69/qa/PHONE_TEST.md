# v0.5.69 Phone Test — Archive Intake

Status: **PENDING**

## Delivery gate

Use only the established Renault Menu path:

1. `5 — Оновити проєкт з GitHub`
2. `19 — Статус проєкту / build`
3. Require exact current main `53f4208b4b2e4d27e4bb55094c855eaf13fb2442`, Tests PASS, APK PASS and `✓ МОЖНА ЗАВАНТАЖУВАТИ`.
4. `8 — Download APK для поточного commit`
5. `13 — Відкрити папку останнього APK`
6. Install over the current app. Do not uninstall or clear data.

## Gate A — install/data preservation

- [ ] App shows v0.5.69 / build 85.
- [ ] Existing projects and installed volumes are still present.
- [ ] Open one known installed volume.

## Gate B — first real ZIP archive

Use a real old Renault ZIP archive.

1. Open the target Project.
2. Tap `Створити .rdpkg з архіву`.
3. Select the ZIP through Android SAF.
4. Choose the normal Renault .rdpkg destination folder.
5. Observe archive intake status/progress.
6. If exactly one new volume is detected, it may continue automatically.
7. Result must be one canonical .rdpkg + one installed volume, with no duplicate.
8. Original ZIP must remain present and unchanged.

## Gate C — duplicate ZIP

Use an archive whose exact volume is already installed.

- [ ] App identifies/skips the exact duplicate.
- [ ] Project volume count does not increase.
- [ ] No partial destination package remains.

## Gate D — multi-volume ZIP

Use an archive containing more than one Renault raw volume.

- [ ] App enters `Архів · вибір томів`.
- [ ] No volume is silently auto-selected as “the first”.
- [ ] Already-installed volumes are marked and disabled/unselected.
- [ ] Select one or several new volumes.
- [ ] Continue; only selected/new volumes are prepared/imported.
- [ ] Rotate while chooser is open and confirm the same selection/list returns without re-extraction.

## Gate E — lifecycle / lock / cancel

During a real extraction/conversion:
- [ ] background Renault Docs;
- [ ] lock the phone for several minutes;
- [ ] return and confirm the same operation continues/completes;
- [ ] repeat with Cancel and confirm clean CANCELLED terminal state;
- [ ] no duplicate rerun after recreation.

## Optional format smoke after ZIP

If real samples are available:
- [ ] 7Z opens through the same archive flow.
- [ ] RAR opens through the same archive flow.

Do not close issue #51 until the required ZIP + lifecycle gates pass.

## Phone evidence 2026-10-07 — exact duplicate ZIP

Observed on Megane II:
- archive ultimately identified as `NT8266A · 2004-06-28`;
- terminal result: `Том уже є`;
- message: `Том уже є в проєкті: NT8266A · 2004-06-28. Конвертацію пропущено.`.

Functional duplicate result: **PASS**.

Performance/fast-path finding:
- before the terminal duplicate result, the UI visibly showed `Розпаковую ZIP… Файлів: 3732 / 4378`;
- therefore this archive was not rejected at the pre-extraction stage;
- tracked separately as `PERF-ARCHIVE-001`.

Do not mark the whole Archive Intake release PASS yet; single-new-volume, multi-volume chooser, lifecycle/background/lock/cancel and cleanup gates remain.

