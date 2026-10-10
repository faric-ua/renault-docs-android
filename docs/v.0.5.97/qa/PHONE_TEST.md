# Renault Docs v0.5.97 — safe phone QA

**PENDING.** Do not mark PASS on source/CI alone. No need to modify existing tomes.

1. Install **only trusted stable-signed main v0.5.97/build113** over existing v0.5.96, no uninstall/clear.
2. Check registered project/tomes and open one existing native section / PDF; normal mode unchanged.
3. If already available, open Megane II → «Додати» → «Створити .rdpkg з raw» and use Android SAF to *select* an already existing KangooII-labelled RAW folder (not .rdpkg or original ZIP). Expected explicit «Неправильне джерело» / model conflict **before** destination chooser. No conversion/service/output starts. Press «Зрозуміло», return to project. Do not perform operation if source unavailable.
4. A matching Megane raw folder or model-neutral filename should be allowed to open the destination picker; **cancel picker** without creating packages.
5. Rotation or Back must not rerun anything. Original Kangoo/Megane archives and 20+ tomes unchanged.

Do not use the user’s existing wrongly labelled .rdpkg as a test input to raw conversion. No force-stop, reboot, emulator device_config or repeated import just for a screenshot.

Status gate: PHONE INSTALL/RETENTION separate from wrong-model block behavior. #82/#85 remain open for any missing actual phone evidence. Signed build evidence must be attached before starting.
