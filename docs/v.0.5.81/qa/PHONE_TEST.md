# v0.5.81 / build 97 — unified dialogs phone QA

Status: PENDING — do not close from automated CI alone.

- [ ] Archive preview: compact filename + target + possible duplicate text; only `Скасувати` and `Продовжити` primary decision buttons.
- [ ] Tap `Технічні деталі`: entire provider, actual SAF Document ID and full registered-project NT match summary are visible and scrollable.
- [ ] Expand -> rotate landscape -> same source and expanded details -> rotate portrait -> state preserved; no work launched.
- [ ] Collapse details -> `Скасувати`: no output picker, no conversion, last diagnostic retained.
- [ ] Existing .rdpkg remains unselectable in archive-input picker, Kangoo archive remains blocked inside Megane II.
- [ ] Home project delete confirmation stays DANGER with red affirmative action and Cancel; no deletion during QA.
- [ ] Settings single-choice dialogs share border/buttons, remain functional and survive rotation.
- [ ] Viewer section navigator/search dialog now matches style, search still works and returns to same place on Close.
- [ ] Viewer frame-debug report now matches style; Copy/Close retain their meanings.
- [ ] Long filenames, multiple matches, portrait/landscape, keyboard-under-dialog and small screens checked visually.

**Preservation:** No uninstall, clearing app data, deletion of prepared packages, movement of source ZIP or unrequested native conversion for these checks.
