# v0.5.3 Phone Result → v0.5.4

Date: 2026-09-24

## Functional result

The hybrid full-frameset runtime solved the legacy navigation problem.

User confirmation:
- section controls work;
- combo/select works;
- the legacy runtime is functionally correct.

## UX finding

Modern section navigation visibly opens the Classic Renault frameset.

This is not a runtime failure. It is a presentation-boundary failure:
Modern owns the outer navigation, but Classic is still visible while the internal legacy runtime is being initialized and used.

## v0.5.4 direction

Keep v0.5.3 runtime architecture unchanged because it restored functionality.

Add a Modern hybrid shell:
- hide the WebView during legacy frameset initialization;
- show a Modern loading status;
- wait until the requested legacy section is selected;
- aggressively collapse the old three-digit section navigation frame(s) to zero size while keeping their documents alive;
- reveal the working content only after the hybrid selector reports success;
- on timeout, reveal the runtime as a safe fallback instead of leaving a blank screen.

This preserves original cross-frame JavaScript while preventing Modern from visibly starting in Classic.
