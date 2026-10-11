# Renault Docs v0.5.97 — safe phone QA

**PARTIAL PHONE PASS — 2026-10-11**: user confirmed in-place v0.5.97 installation, retained tomes, and existing documentation opening after requested gate. Wrong-model raw selection and neutral matching source behavior remain PENDING. Do not mark full feature PASS on install smoke alone. No need to modify existing tomes.

1. Install **only trusted stable-signed main v0.5.97/build113** over existing v0.5.96, no uninstall/clear.
2. Check registered project/tomes and open one existing native section / PDF; normal mode unchanged.
3. If already available, open Megane II → «Додати» → «Створити .rdpkg з raw» and use Android SAF to *select* an already existing KangooII-labelled RAW folder (not .rdpkg or original ZIP). Expected explicit «Неправильне джерело» / model conflict **before** destination chooser. No conversion/service/output starts. Press «Зрозуміло», return to project. Do not perform operation if source unavailable.
4. A matching Megane raw folder or model-neutral filename should be allowed to open the destination picker; **cancel picker** without creating packages.
5. Rotation or Back must not rerun anything. Original Kangoo/Megane archives and 20+ tomes unchanged.

Do not use the user’s existing wrongly labelled .rdpkg as a test input to raw conversion. No force-stop, reboot, emulator device_config or repeated import just for a screenshot.

Status gate: PHONE INSTALL/RETENTION separate from wrong-model block behavior. #82/#85 remain open for any missing actual phone evidence. Signed build evidence must be attached before starting.

## User screenshot acceptance — 2026-10-11

- **RAW_TREE explicit cross-model picker gate: PHONE PASS.** Megane II selected; existing Kangoo-named raw source selected through raw preparation. App displayed «Неправильне джерело» with «Джерело містить назву моделі kangoo, але вибрано проєкт «Megane II». Підготовку зупинено.» before any destination picker. Megane II project header shows **21 том**. No package creation observed.
- Install-over / existing volumes / document open: previous user **PHONE PASS**.
- Still untested: arbitrary model-neutral names, worker bypass, all possible ambiguous document metadata, #85 prepared package as original archive, and #40 OS quota callback. Do not assert these PASSED from one screenshot.

## Archive input selector / duplicate preflight PHONE QA — 2026-10-11

- **SAF filter PASS:** already-prepared `.rdpkg` files visible grey/disabled in `Documents/Renault/packages/rdpkg` when picking original archive. They cannot be selected as source. This does **not** invoke the independent app-side bad-extension validation; keep its phone test PENDING (#85).
- **ZIP catalog preflight PASS:** genuine `Megane II B,C,S 84 Europe_NT8343_Visu v4.0_2007.05.02.zip` is selectable, and app warns a possible already installed matching NT8343/2007-05-02 with clear `Вміст архівів не порівнювався`. Cancel, don't produce duplicate packages. No conversion, data movement or cleanup occurred in supplied evidence.
- Next safe gate: #79 Home «Новий том» project chooser destination Add panel, no auto .rdpkg picker, no actual source chosen.
