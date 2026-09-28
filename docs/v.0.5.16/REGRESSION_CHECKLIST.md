# v0.5.16 Regression checklist

## Connector identity

- [ ] `101_1` exports as `101_1(pines).pdf`.
- [ ] `101_2` exports as `101_2(pines).pdf`.
- [ ] No code invents `_1/_2/_3` as duplicate-file counters.
- [ ] If available, `120_1/120_2/120_3` preserve their exact source IDs.

## v0.5.15 table behavior

- [ ] Abbreviation columns remain content-aware.
- [ ] Wrapped E2/E3-style rows vertically center short cells.
- [ ] 2-column PDF remains portrait.
- [ ] Pin/contact table remains readable.
- [ ] Cyrillic remains correct.
- [ ] Multi-page export keeps all rows.

## Connector UI

- [ ] `Схема + піни розʼєма` appears when both parts exist.
- [ ] Combined action opens the original connector composition.
- [ ] Separate scheme action still works.
- [ ] Separate pin description action still works.

## Dataset/package

- [ ] No Runtime IR regeneration required.
- [ ] Renault Menu → 9 is not needed for this APK-only release.
