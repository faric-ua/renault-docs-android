# Renault Docs v0.3.3 — PDF zoom picker

## Purpose

Improve PDF zoom controls without changing the dataset or Fast Pack format.

## PDF toolbar

Implemented:
- editable zoom field;
- tap the percentage field and enter a custom value;
- accepted manual range: 50%–200%;
- +/- buttons change zoom by 10%;
- dropdown preset button on the right of the field;
- presets: 85%, 100%, 120%, 150%, 200%;
- outside tap closes the preset menu;
- toolbar spacing is compact for phone-width screens;
- zoom still changes the actual displayed page width and rerenders at an appropriate resolution.

## Compatibility

No dataset rebuild is required.
Fast Pack remains unchanged.
PDF Save remains unchanged.
