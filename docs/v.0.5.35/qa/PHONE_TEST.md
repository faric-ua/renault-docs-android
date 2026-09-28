# v0.5.35 Phone test — rotation fullscreen preservation

## Prepare

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Install v0.5.35.
4. Point 9 is NOT required.

## Gate A — single PDF fullscreen

1. Open one PDF.
2. Enter fullscreen.
3. Rotate portrait → landscape.
4. Rotate landscape → portrait.

Expected:
- fullscreen remains active throughout;
- app toolbar never returns during the rotation;
- fullscreen button remains active;
- PDF page/zoom/scroll stays in place.

## Gate B — two-document fullscreen

1. Open Companion.
2. Enter fullscreen.
3. Move the divider away from default.
4. Rotate portrait ↔ landscape several times.

Expected:
- fullscreen remains active;
- both documents remain open;
- toolbars stay hidden by split focus;
- divider ratio remains;
- both PDF states remain intact.

## Gate C — controls after rotation

In two-document fullscreen:
- rotate;
- double tap either document.

Expected:
- temporary overlay controls appear;
- fullscreen button/state is still correct;
- controls auto-hide again.

## Gate D — exit

After one or more rotations:
- press fullscreen once.

Expected:
- one tap exits fullscreen;
- main app toolbar appears;
- fullscreen button becomes inactive.

## Regression

Verify:
- Companion close still works;
- draggable divider still works;
- 300–400% main PDF scroll remains stable.

## Closeout

v0.5.35 is PASS when rotation no longer exits or visually deactivates fullscreen for either one or two documents.
