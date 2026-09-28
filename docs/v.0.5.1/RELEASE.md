# Renault Docs v0.5.1 — Legacy standalone compatibility

## Purpose

Phone follow-up for the first native Technical Blue section navigation wave.

v0.5.0 successfully opens sections directly from the native 101/103/... list, but real Renault HTML reveals two legacy assumptions:

1. some pages were designed to inherit a white browser/frame canvas and therefore render black text on a transparent background;
2. some links/forms still target named frames that existed only inside the original Renault frameset.

## Implemented

The compatibility layer runs only when legacy content is opened directly from the native section flow.

### Transparent legacy pages

If both html/body backgrounds are transparent:
- inject a white canvas;
- force light color-scheme for that legacy document.

Pages that explicitly paint their own blue/other background are left untouched.

### Missing named frame targets

For direct standalone legacy pages:
- anchors/forms/base elements with a named target are inspected;
- if that named frame does not exist, the target is rewritten to `_self`;
- dynamically inserted target-bearing elements are handled by a MutationObserver;
- named `window.open(url, oldFrameName)` calls fall back to current-page navigation when that frame no longer exists.

Standard targets such as `_self`, `_top`, `_parent`, `_blank` are preserved.

## Important boundary

This is a compatibility bridge, not a full rewrite of inner Renault UI.

Interactive legacy widgets such as:
- select boxes;
- engine thumbnails;
- document selectors;
- PDF links

remain legacy for now, but should function more reliably when opened from native sections.

## Unchanged

- native 101/103/... section list stays;
- Classic fallback stays;
- PDF rendering/export stays;
- no dataset/Fast Pack schema change;
- point 9 is not required for this APK-only fix.
