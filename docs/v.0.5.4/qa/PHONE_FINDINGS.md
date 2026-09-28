# v0.5.4 Phone Finding

Date: 2026-09-24

## Result

v0.5.4 did not achieve the intended Modern presentation boundary.

Observed on the real phone after opening a section from Modern:

- Android header remained present;
- title became `Visu Schema`;
- status showed:
  `Modern shell: Classic runtime не вдалося повністю сховати.`
- fallback revealed the Classic Renault root/splash screen;
- visible content showed the yellow Laguna 2 / Renault / NT8183A / 22.01.2001 page.

## Interpretation

This is not evidence that the legacy runtime is broken.

v0.5.3 already confirmed:
- legacy section controls work;
- combo/select works;
- full frameset runtime is the correct compatibility base.

The v0.5.4 failure is specifically:
- hybrid-ready detection / section auto-selection / frame-collapse did not complete;
- timeout fallback correctly revealed the Classic runtime instead of leaving a blank screen.

Status:
`PHONE_FAIL_MODERN_PRESENTATION_RUNTIME_OK`

## Next step

Do not add another blind collapse heuristic.

First add a diagnostic frame-tree report on the real device and capture:
- frame nesting;
- window names;
- src/current URLs;
- frameset rows/cols;
- visibility/dimensions;
- section-code counts;
- select/button/image counts;
- which frame owns the old 101/103 menu;
- which frame owns the working combo/content.

After that, implement deterministic frame hiding/projection based on actual Renault structure.
