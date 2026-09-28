# v0.5.1 Phone Findings → v0.5.2

Date: 2026-09-24

## CMP101 legacy navigation

Observed on the real phone:
- top legacy action 3 loads additional content;
- actions 1, 2 and 4 do not react;
- a legacy combo/select allows choosing an item but does not navigate to its content;
- the page itself and its local icons/assets render.

Interpretation:
the remaining failures are navigation-context failures rather than missing assets. The page still expects old parent/top frame objects and/or inline JavaScript handlers.

## v0.5.2

Adds three compatibility mechanisms for standalone legacy pages:

1. Hidden frame bridge
   - scans inline HTML and same-origin external scripts for named and numeric frame references;
   - creates hidden same-origin iframe placeholders;
   - mirrors navigation performed inside those placeholders into the main viewer.

2. Select fallback
   - lets the original onchange handler run first;
   - if main navigation did not happen, examines selected option value / inline handler for a local target and opens it in the main viewer.

3. Click fallback
   - lets original click behavior run first;
   - if no main navigation occurred, extracts local HTML/PDF candidates from href/onclick and opens them.

## Safety

Fallback candidates must resolve to the same local virtual origin.
Classic full frameset mode is unchanged.
No dataset/Fast Pack refresh is required.
