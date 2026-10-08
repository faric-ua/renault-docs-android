# Findings

- Home currently shows separate Ready Projects, Tools and explanation outside Add; inconsistent with project-level pinned/collapsible Add.
- Global system bars rule exists in RenaultDocsApplication + Ui but requires coverage across configChanges-aware screens and focus transitions.
- Landscape Home pinned/expanded Add must not obscure project list; create bounded scroll and preserve portrait pin state.

## QA-ROUTE-005 — Invalid Viewer `Розділи` test path (2026-10-08)

**Finding type:** test instruction / route mismatch, not a confirmed app defect. Screenshots show actual original Renault **Visu Schema / Classic Viewer** with orange NT:8340 entry page, search `120` first `0/0` on that page, then `1/3` in Classic content and PDF pane. The expected `Розділи · …` modal never appears on this path.

**Code evidence:** `ViewerActivity.kt` renders toolbar button `Розділи` only when `modernClassicEntrypoint` is nonblank **and** `hybridSectionMode` is true (section code nonblank, entrypoint equals `modernVolumeEntrypoint`). The same conditional toolbar otherwise says `Modern` or is absent. `NativeSectionActivity.openLegacyFallback()` deliberately launches untouched Classic via `ViewerActivity.intentForEntrypoint(..., title, entrypoint, treeUri)` and explicitly excludes the former hybrid projection flow. `ModernVolumeActivity.openSection()` enters `NativeSectionActivity`, not that hybrid Viewer. Thus the previously specified Modern → Classic → Viewer “Розділи” expectation cannot be relied upon.

**Disposition:** test 5 = **INVALID TEST ROUTE / NOT PASS / NOT FAIL**. Do not modify original Classic or add a redundant `Розділи` button just to make the test pass. Keep dormant/legacy hybrid dialog code assessment as a separate source cleanup decision. Next QA: use the actual on-screen PDF fullscreen `⛶` control in Classic Viewer, test portrait-landscape-portrait preservation, and return to the same PDF/document. Separate search-result consistency investigation only if user explicitly wants it; a `0/0` match on landing page and `1/3` on a later content frame do not alone establish a search bug.

## UX-ADD-VISIBILITY — Possible overlap in Home (2026-10-09)

**Unconfirmed observation, not a reproduced bug.** In a screenshot with Home `Додати` expanded/pinned, its lower edge is near the upper portion of the Megane II tile and the `Мої Renault` heading is not visible. This may be the independent list retaining a scrolled position under a fixed panel. Request a non-destructive check: collapse `Додати` with ▲, confirm `Мої Renault` heading and full Megane II card can be revealed, then expand and scroll the project list. Change no code until visibility is reproduced as a failure.


### Video follow-up — 2026-10-09

The user provided a ~32-second video showing expanded and collapsed Home Add states, accessible `Мої Renault` header, Megane II / Laguna II / Kangoo II project tiles, and the same Add toggle inside Megane II. The first project remains accessible after collapse. **UX-ADD-VISIBILITY = NOT REPRODUCED, NO CODE CHANGE NECESSARY.** Do not treat the screenshot's apparent border proximity as proven overlap. Manual UI QA is on hold by user request.
