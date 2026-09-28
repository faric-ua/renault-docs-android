# v0.5.15 Regression checklist

## Modern section shell

- [ ] Back works.
- [ ] Home works.
- [ ] Search opens current volume search.
- [ ] Settings opens.
- [ ] Modern/Classic segmented control still works.
- [ ] Primary tiles remain stable between 101 and 107.
- [ ] 107 keeps disabled Connector tile.

## Tables

- [ ] 101 abbreviations render as a 2-column bordered table.
- [ ] Short code column is visibly narrower than v0.5.14 where appropriate.
- [ ] Long description uses remaining width.
- [ ] Wrapped rows keep one full-height left cell.
- [ ] Short code text is vertically centered in tall rows.
- [ ] Connector pin table still preserves all source columns.

## Table PDF export

- [ ] Save picker opens.
- [ ] Suggested filename is meaningful.
- [ ] 2-column glossary PDF is portrait.
- [ ] 4-column pin PDF remains readable.
- [ ] Cyrillic renders.
- [ ] Saved file opens.
- [ ] Multi-page export works for a long table.
- [ ] Cancel returns to the same Modern document.

## Connector

- [ ] Combined `Схема + піни розʼєма` appears only when both parts exist.
- [ ] Combined action opens the original connector composite.
- [ ] Separate scheme action still works.
- [ ] Separate pin description action still works.
- [ ] Classic fallback remains untouched.

## PDF viewer

- [ ] Existing ordinary PDF viewer still opens.
- [ ] Zoom toolbar remains usable.
- [ ] Save-original-PDF behavior is unchanged.

## Dataset/package

- [ ] No Runtime IR regeneration required for this release.
- [ ] Existing v0.5.9+ shards continue to load.
