# v0.5.19 Regression checklist

## Converter / Runtime IR

- [ ] Pin table detection does not require a `T_` filename.
- [ ] First body row is never discarded.
- [ ] Visual-only leading header rows are replaced by one semantic header.
- [ ] Unrelated 4-column tables remain unchanged.
- [ ] Semantic header is `№ | мм² | Код | Опис`.

## Native layout

- [ ] `0.6` remains one line.
- [ ] `1.4`, `3CV`, `3N` remain one line.
- [ ] `B74,K74` does not wrap unnecessarily.
- [ ] `СОКРАЩЕНИЯ` width is considered.
- [ ] Last column receives the remaining width.

## PDF

- [ ] Pin header visible on page 1.
- [ ] Header repeats after page break.
- [ ] Compact columns are content/header-sized.
- [ ] Last description column receives remaining width.
- [ ] Exact `(pines)` filename preserved.

## Package workflow

- [ ] Point 9 required after install.
- [ ] Reduced 3-volume test package works when extra converted volumes are moved outside build root.
- [ ] Full volume set restored + point 9 rerun before final release validation.
