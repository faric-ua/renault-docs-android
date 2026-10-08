# Renault Docs v0.5.82 — Phone QA report — 2026-10-08

## Environment and build
- User reports testing installed Renault Docs v0.5.82 / build98 after signed APK #145 was provided.
- Runtime source SHA: `f17b319c4e17d8f7005ae20221dd7bd642cb172b`; APK #145 main CI PASS.
- User confirmation: **«Пасс. Взагалі все чудово.»**
- Evidence type: user-reported phone acceptance following explicit Home portrait→landscape→portrait test instructions. No new screenshot submitted in the acceptance message.

## Accepted core scenario (PASS)
- Home has the collapsible/pinnable Add panel and My Renault beneath it.
- Add expanded and portrait 📌 pinned; in landscape system status/navigation bars hide and Add collapses; on return to portrait system bars and previous pin state return.
- No defects were reported for this tested scenario.

## Not independently evidenced
- Per-dialog fullscreen or transient swipe-on-demand, Settings/Viewer/ModernVolume, explicit PDF fullscreen.
- Expandable Home Add content (Ready Projects/Google Drive, Legacy, Tools), detailed independent Add/Projects scroll and terminal status interactions.
- Cross-app system SAF picker behavior and IME; destructive/project import/export operations; volumes count after install and data-preservation checks.
- Older v0.5.81 Viewer-specific `Розділи` modal QA remains distinct.

## Result
**CORE PHONE PASS / EXTENDED QA OPEN**. Do not close the whole release or unrelated issue set yet. Next isolated test should inspect Home Add choices and independent scrolling without starting any import, followed by help/dialog orientation and remaining route checks.
