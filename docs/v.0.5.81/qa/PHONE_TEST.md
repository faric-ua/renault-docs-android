# v0.5.81 / build 97 — unified dialogs phone QA

Status: PHONE QA IN PROGRESS — compact source dialog rotation + Cancel/no-run **PASS reported by user** on 2026-10-08; remaining UI dialogs pending.

- [x] Archive confirmation flow and short-window presentation accepted verbally by user (`++` then `Пасс`). Detailed independent visual checklist not fully evidenced.
- [ ] Full provider/Document ID/NT list visibility and scroll behavior **not individually confirmed**, despite PASS for expanded-state preservation.
- [x] Rotate with `Технічні деталі` expanded -> state preserved, no automatic conversion — **PASS explicitly confirmed**.
- [x] `Скасувати` after source preview -> **no conversion started — PASS explicitly confirmed**. [Further verification still needed: previous diagnostic remains and no destination chooser appears.]
- [ ] Existing .rdpkg remains unselectable in archive-input picker, Kangoo archive remains blocked inside Megane II.
- [ ] Home project delete confirmation stays DANGER with red affirmative action and Cancel; no deletion during QA.
- [ ] Settings single-choice dialogs share border/buttons, remain functional and survive rotation.
- [ ] Viewer section navigator/search dialog now matches style, search still works and returns to same place on Close.
- [ ] Viewer frame-debug report now matches style; Copy/Close retain their meanings.
- [ ] Long filenames, multiple matches, portrait/landscape, keyboard-under-dialog and small screens checked visually.

**Preservation:** No uninstall, clearing app data, deletion of prepared packages, movement of source ZIP or unrequested native conversion for these checks.

## Device evidence — 2026-10-08

User confirmed `Пасс` in response to the explicit joint check: on rotation the open `Технічні деталі` state remained, and after `Скасувати` no conversion started. Earlier `++` acknowledged the short-dialog test. Record these observed behaviors as phone PASS without claiming all warning/help/viewer windows have been visually approved. Next: Viewer section navigator / Help / Settings / destructive confirm visuals, with no actual destructive action.
