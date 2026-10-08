# v0.5.82 Phone QA — CORE HOME/ORIENTATION PHONE PASS; EXTENDED QA OPEN

- [x] Home: system bars hidden in landscape, restored in portrait — PHONE PASS (2026-10-08). [x] Home `?` Help dialog also survives portrait→landscape→portrait, system bars hide/restore, and Close returns to Home without automatic action — PHONE PASS (2026-10-08). [ ] Other Activity/dialog windows and transient swipe not yet checked.
- [ ] Android SAF picker and system keyboard remain controlled by Android.
- [x] Home Add collapses/expands; portrait 📌 pin/expanded state returns after landscape rotation — PASS on phone, user response «Пасс. Взагалі все чудово.» (2026-10-08).
- [x] User confirmed Home Add action availability: `Новий том`, `Новий проєкт`, `Готові проєкти`, `Конвертер`, `Legacy` — PASS (2026-10-08). [ ] Independently inspect explanatory text and active-operation status later.
- [x] Home: Add + My Renault general layout accepted by user (2026-10-08). [ ] Exact counters and legacy navigation not separately demonstrated in v0.5.82.
- [x] Add content scrolls separately from `Мої Renault` project list — PASS, user confirmed (2026-10-08). [ ] Small-screen landscape clipping not separately documented.
- [ ] Cancel SAF new volume, no automatic import; existing prepared export progress/terminal remains accessible while Add collapsed.
- [x] Settings screen portrait→landscape→portrait system bars restore; one non-destructive choice dialog remains open during rotation and closes without changing preferences — PHONE PASS (2026-10-08, user «Пасс»). [ ] Viewer / ModernVolume / PDF fullscreen remain untested.
- [ ] Install over existing app, do not clear data, move or delete archives.

## Acceptance evidence — 2026-10-08

User confirmed «Пасс. Взагалі все чудово.» in direct response to the initial v0.5.82 on-phone scenario: Home Add + My Renault layout, pin/expanded state, portrait→landscape hidden system bars, portrait restore. Record as **core scenario PHONE PASS**. User did not provide screenshots for v0.5.82 and did not separately attest to all further Help/Viewer/Settings/SAF/legacy/status flows. Those remain on the checklist and should not be silently closed.

## Additional acceptance — 2026-10-08 (second test)

User: «Все добре, пас, поїхали далі.» in response to checking the expanded Home `Додати` choices (New Volume, New Project, Ready Projects, Converter, Legacy) and independent scrolling of Add content vs. `Мої Renault`. Record as **PHONE PASS** for these visible UI/accessibility scenarios. Does not demonstrate execution of these actions, any SAF import, Google Drive download, active status/cancel, or Viewer/Settings/Help modal orientation. Next isolated test: Home `?` Help modal portrait→landscape→portrait; no auto-action and system bars restore.

## Home Help dialog — test 3 PASS (2026-10-08)

User answered «Пасс» to the explicit five-step check of Home `Додати` → `?` Help window: remains open across rotation, app-owned system bars hidden landscape/restored portrait, no clipped controls, Close returns Home with no automatic operation. PASS applies to this specific Help window only. Next isolated test: Settings screen portrait→landscape→portrait, verify bars, then open and close a non-destructive Settings choice dialog without saving changes. Keep Viewer/PDF fullscreen, other modal dialogs, import and destructive flows pending.

## Settings orientation / choice dialog — test 4 PASS (2026-10-08)

User responded «Пасс» to Settings: orientation portrait→landscape→portrait, system bars hide/restore, open a harmless selection dialog, rotate again and close without saving. Record as **PHONE PASS for Settings and selected choice dialog**; do not treat Viewer/PDF, external SAF, destructive actions or persistent operation-status UI as verified. Next: Viewer-specific `Розділи` modal, not the separate ModernVolume native section list.
