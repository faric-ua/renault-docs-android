# v0.5.82 Phone QA — CORE HOME/ORIENTATION PHONE PASS; EXTENDED QA OPEN

- [x] Home: status and navigation bars hidden in landscape, visible again in portrait — PASS on phone (2026-10-08). [ ] Other app-owned screens, dialog windows and transient swipe remain unverified.
- [ ] Android SAF picker and system keyboard remain controlled by Android.
- [x] Home Add collapses/expands; portrait 📌 pin/expanded state returns after landscape rotation — PASS on phone, user response «Пасс. Взагалі все чудово.» (2026-10-08).
- [x] User confirmed Home Add action availability: `Новий том`, `Новий проєкт`, `Готові проєкти`, `Конвертер`, `Legacy` — PASS (2026-10-08). [ ] Independently inspect explanatory text and active-operation status later.
- [x] Home: Add + My Renault general layout accepted by user (2026-10-08). [ ] Exact counters and legacy navigation not separately demonstrated in v0.5.82.
- [x] Add content scrolls separately from `Мої Renault` project list — PASS, user confirmed (2026-10-08). [ ] Small-screen landscape clipping not separately documented.
- [ ] Cancel SAF new volume, no automatic import; existing prepared export progress/terminal remains accessible while Add collapsed.
- [ ] Settings/Viewer/dialog orientation and ongoing playback/fullscreen behavior not regressed.
- [ ] Install over existing app, do not clear data, move or delete archives.

## Acceptance evidence — 2026-10-08

User confirmed «Пасс. Взагалі все чудово.» in direct response to the initial v0.5.82 on-phone scenario: Home Add + My Renault layout, pin/expanded state, portrait→landscape hidden system bars, portrait restore. Record as **core scenario PHONE PASS**. User did not provide screenshots for v0.5.82 and did not separately attest to all further Help/Viewer/Settings/SAF/legacy/status flows. Those remain on the checklist and should not be silently closed.

## Additional acceptance — 2026-10-08 (second test)

User: «Все добре, пас, поїхали далі.» in response to checking the expanded Home `Додати` choices (New Volume, New Project, Ready Projects, Converter, Legacy) and independent scrolling of Add content vs. `Мої Renault`. Record as **PHONE PASS** for these visible UI/accessibility scenarios. Does not demonstrate execution of these actions, any SAF import, Google Drive download, active status/cancel, or Viewer/Settings/Help modal orientation. Next isolated test: Home `?` Help modal portrait→landscape→portrait; no auto-action and system bars restore.
