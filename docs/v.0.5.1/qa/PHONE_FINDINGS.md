# v0.5.0 Phone Findings → v0.5.1

Date: 2026-09-24

## What works

Real-phone testing confirms that the first native section wave can open several kinds of Renault content:
- PDF content opens correctly;
- legacy select/dropdown pages can navigate when their own controls are self-contained;
- image/engine-choice pages render their local assets;
- PDF viewer and Technical Blue top controls remain functional.

## LEGACY-CONTENT-001 — transparent page on dark WebView canvas

Observed on CMP141:
legacy table/text uses black foreground but the page itself has no explicit background. The app's dark WebView background therefore shows through and makes the content difficult to read.

v0.5.1:
When a legacy HTML page is opened standalone from native section navigation and both html/body are transparent, inject a white canvas and light color-scheme.

Explicit Renault blue backgrounds are not overridden.

## LEGACY-NAV-001 — old named frame targets

Observed:
Some legacy pages render but some internal actions do not navigate when opened directly from native sections.

Likely compatibility cause:
the original Renault page expected named sibling/parent frames from the old frameset.

v0.5.1:
- rewrite missing named targets on anchors/forms/base to _self;
- observe dynamically inserted target elements;
- named window.open calls targeting a missing frame fall back to current-page navigation.

## Remaining boundary

If a page uses hard-coded JavaScript such as direct parent.frames[...] assignment instead of normal target/window.open navigation, it can still require a section-specific bridge.

For remaining failures, record the exact section code/title and the exact control that does nothing.
