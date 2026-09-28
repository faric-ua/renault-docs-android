# v0.5.29 Phone test — volume documentation + fullscreen active state

## Prepare

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Install v0.5.29.
4. Renault Menu → 9 — REQUIRED.

## Gate A — fullscreen pressed state

Open a PDF and enter fullscreen.

Expected:
- fullscreen button visibly changes to active/accent state;
- exiting fullscreen restores normal button state;
- Android Back while fullscreen also restores the inactive state;
- rotation/recreation does not leave the button visually out of sync with the actual fullscreen state.

## Gate B — section startup

Open several sections in one volume without opening `Документація`.

Expected:
- sections open normally;
- Schemes / Connector / Position behavior is unchanged;
- no documentation-loading status is shown during ordinary section startup.

## Gate C — shared documentation inside one volume

Use one volume and open `Документація` from section 105.

Expected:
- button opens the same three volume-owned categories visible before migration where available:
  - Загальна документація;
  - Запобіжники;
  - Довідка.
- opening their child documents works normally.

Return to another section in the SAME volume and open `Документація`.

Expected:
- it exposes that same volume's documentation set;
- section code/title does not change which documentation files belong to the volume.

## Gate D — volume isolation

Open a DIFFERENT Renault volume/configuration and then `Документація`.

Expected:
- it uses that second volume's own files;
- there is no cross-volume reuse or navigation into the previous volume.

## Gate E — fallback/regression

Check at least one section with normal Schemes and one PDF.

Expected:
- no section menu regression;
- PDF 50–400% zoom and fullscreen continue to work;
- no Runtime IR missing-path error after point 9.

## Closeout

v0.5.29 is PHONE PASS when documentation is shared only within one volume, isolated between volumes, and fullscreen active state matches the real mode.
