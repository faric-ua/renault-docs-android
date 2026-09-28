# v0.5.34 Phone test — fullscreen state reconciliation

## Prepare

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Install v0.5.34.
4. Point 9 is NOT required.

Use the same main schematic + Companion workflow from v0.5.33.

## Primary reproduction gate

1. Open main PDF.
2. Open Companion.
3. Enter fullscreen with two documents.
4. Close the second document.
5. Rotate landscape → portrait.

Expected:
- fullscreen state remains exactly what it was before rotation;
- if still fullscreen, the fullscreen button stays active/blue and app toolbar stays hidden;
- if fullscreen was exited, the button is inactive and app toolbar is visible;
- there is no mismatch between button state and app chrome.

## Exit fullscreen gate

From the single-document state after the sequence above:
- tap fullscreen once.

Expected:
- one tap is enough to exit fullscreen;
- app toolbar/header appears immediately;
- button becomes inactive;
- no second corrective tap is required.

## Reverse gate

Repeat, but exit fullscreen before closing Companion.

Expected after close + rotation:
- main app toolbar remains visible;
- fullscreen button remains inactive;
- PDF remains visible and keeps page/zoom/scroll state.

## Regression

Verify v0.5.33 behavior still works:
- draggable divider;
- double-tap controls;
- auto-hide controls;
- two-document fullscreen;
- rotation keeps split ratio and both document states.

## Closeout

v0.5.34 is PASS when Android fullscreen chrome and the PDF fullscreen button always represent the same state across Companion close and rotation.
