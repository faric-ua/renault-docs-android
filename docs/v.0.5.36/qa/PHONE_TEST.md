# v0.5.36 Phone test — fullscreen button acknowledgement

## Prepare

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Install v0.5.36.
4. Point 9 is NOT required.

Use the exact sequence from the v0.5.35 video.

## Gate A — single PDF rotation

1. Open one PDF.
2. Enter fullscreen.
3. Rotate portrait → landscape.
4. Wait briefly.
5. Rotate landscape → portrait.

Expected:
- app remains fullscreen throughout;
- main app toolbar stays hidden;
- PDF fullscreen button remains visibly active/blue after each rotation;
- no corrective tap is required.

## Gate B — exit after rotation

After Gate A:
- tap fullscreen once.

Expected:
- one tap exits fullscreen;
- app toolbar immediately appears;
- fullscreen button becomes inactive.

## Gate C — two-document rotation

1. Open Companion.
2. Enter fullscreen.
3. Rotate several times.

Expected:
- split focus remains;
- both PDF toolbar fullscreen buttons, when temporarily shown, represent the active fullscreen state correctly;
- no toolbar/button visual desync.

## Gate D — stale retry safety

Rapidly:
- rotate;
- show controls;
- exit fullscreen;
- immediately re-enter fullscreen.

Expected:
- old delayed sync cannot flip the button back to the previous state;
- button always converges to the latest Android fullscreen state.

## Regression

Verify:
- draggable divider;
- double-tap controls;
- page/zoom/scroll preservation;
- 300–400% main PDF remains stable.

## Closeout

v0.5.36 is PASS when Android fullscreen and every visible PDF fullscreen button always converge to the same state after rotation.
