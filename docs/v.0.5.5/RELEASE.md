# Renault Docs v0.5.5 — Frame-tree diagnostics

## Purpose

v0.5.4 proved that the full Renault runtime is still visible because the current hide/projection logic does not understand the real frame hierarchy well enough.

v0.5.5 deliberately does not add another hiding heuristic.

It adds a real-device diagnostic report so the next Modern-shell fix can be deterministic.

## New temporary diagnostic action

When a frame-dependent section is opened from Modern, the viewer header shows:

`DBG`

Tap it to collect a structured report of the currently loaded Renault runtime.

The report includes, for every reachable same-origin frame/window:
- tree path and depth;
- index inside parent;
- `window.name`;
- document title;
- full URL and relative path;
- frame element name/id/src;
- frame width/height attributes;
- live bounding rectangle;
- computed display/visibility/opacity;
- parent frameset `rows` / `cols`;
- child frame count;
- detected three-digit Renault section codes;
- select/combo count and selected values;
- PDF-link count;
- image count;
- button/onclick count;
- body background;
- short text fingerprint;
- role hints for menu/combo/content candidates.

It also includes:
- requested Modern section;
- legacy root/fallback entrypoints;
- current hybrid state (`done`, attempts, menu-window presence);
- summary lists of menu candidates and combo candidates;
- any same-origin frame access errors.

## UI

The report opens in a selectable monospaced dialog.

Actions:
- `Копіювати` — copies the full JSON/text report to Android clipboard;
- `Закрити`.

## Important

v0.5.5 is a diagnostic wave.

Do not judge Modern-shell success from this build. The purpose is to capture the real Renault frame structure from the phone.

No dataset/Fast Pack refresh is required.
