# Renault Docs v0.5.7 — Live Modern section navigator

## Goal

Keep the working v0.5.6 projected legacy runtime alive while allowing fast surfing between 101/103/... sections from a searchable Modern panel.

## Viewer navigation

In hybrid section mode the existing `Modern` toolbar action becomes `Розділи`.

Tapping `Розділи`:
- reads the existing `_renault/modern-sections.json` index;
- opens a searchable Modern section panel over the current viewer;
- shows the active section with a check mark;
- filters by code or title;
- keeps a `Повний Modern` action for returning to the native volume screen.

Selecting another section does not create a new ViewerActivity and does not reload the top-level volume frameset.

The already-loaded named frame `org` is used to trigger the requested code. The switch is considered complete only after named `menu` / `nav` expose the requested section.

## Lifecycle

The current section code and section legacy entrypoint are saved across Activity recreation, so rotation does not revert the Modern panel highlight to the originally-opened section.

## Performance diagnostics

DBG now also records:
- active section after live switching;
- Fast Pack preparation duration;
- whether Fast Pack had to be copied into local cache;
- top-document Navigation Timing;
- initial hybrid projection duration;
- latest live section switch attempts / trigger count / duration.

This lets us separate initial legacy-runtime startup cost from section-switch cost before changing the converter/runtime architecture.

## Important

No dataset/Fast Pack refresh is required for v0.5.7.

The existing v0.5.6 deterministic projection remains the runtime foundation.
