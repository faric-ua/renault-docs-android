# v0.5.18 Regression checklist

## Runtime IR

- [ ] Empty legacy table cells are preserved.
- [ ] 4-column `T_*.HTM` pin table gets semantic header.
- [ ] Actual first data row is not replaced.
- [ ] Legacy visual header row is not duplicated.
- [ ] Non-`T_` tables do not receive pin headers.

## Native UI

- [ ] `0.6` remains one line.
- [ ] First three pin columns remain compact/single-line.
- [ ] Long description wraps.
- [ ] Header is visible.

## PDF

- [ ] Header is visible on page 1.
- [ ] Header repeats on page 2+.
- [ ] Compact values do not split.
- [ ] `103_5(pines).pdf` naming remains exact.

## Packaging

- [ ] Point 9 regenerates Runtime IR/package successfully.
- [ ] Dataset opens after regeneration.
