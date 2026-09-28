# Renault Docs v0.4.5 — PDF zoom preset menu hotfix

## Problem

On the real phone the small dropdown button next to the manual PDF zoom field did not visibly open the preset list.

Expected presets:
- 85%
- 100%
- 120%
- 150%
- 200%

## Root cause

The popup was absolutely positioned inside the PDF toolbar.

The toolbar became horizontally scrollable in the previous responsive-toolbar work. In Android WebView this scroll/overflow container clips the popup, so the button receives the click but the menu is not visible.

## Fix

- zoom preset popup uses a fixed overlay;
- popup is moved to document body, outside the scrollable toolbar;
- its position is calculated from the dropdown button on every open;
- position is clamped to the visible viewport;
- if there is insufficient room below, it opens above the button;
- toolbar scroll and window resize reposition the popup;
- clicking outside closes it;
- selecting 85/100/120/150/200 applies the zoom and closes the menu.

No PDF rendering, dataset or Fast Pack format changes.
