# Renault Docs v0.5.2 — Legacy combo and frame-JS bridge

## Phone finding

On CMP101:
- legacy action 3 opens content;
- actions 1, 2 and 4 do not navigate;
- legacy combo/select control changes selection but does not open the selected content.

This confirms that the page and local assets load, but some controls still depend on old frameset JavaScript.

## Fix

Standalone legacy pages now get an additional compatibility bridge.

### Missing frame objects

The page is scanned for old JavaScript references such as:
- `parent.frames["name"]`
- `top.frames["name"]`
- `frames[0]`, `frames[1]`, ...
- `parent.someFrame.location`

For missing targets the bridge creates hidden same-origin iframe placeholders.

When old Renault JavaScript navigates one of those placeholders, its resulting local URL is mirrored into the main viewer.

External same-origin script files are also scanned asynchronously for frame references.

### Select/combo fallback

For legacy `select` controls:
- normal onchange logic gets first chance to run;
- if the main page did not navigate, the bridge examines the selected option value and inline onchange code;
- a local HTML/PDF candidate is opened in the current viewer.

### Click fallback

For anchors/buttons/images:
- normal legacy click logic gets first chance;
- if navigation did not happen, the bridge examines href/onclick code for a local HTML/PDF target and opens it in the current viewer.

## Safety

- fallback navigation is restricted to the current local virtual origin;
- standard targets (_self/_top/_parent/_blank) are not rewritten by the frame bridge;
- Classic full frameset mode remains untouched;
- PDF behavior is unchanged;
- no dataset/Fast Pack refresh is required.
