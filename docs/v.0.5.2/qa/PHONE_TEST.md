# v0.5.2 Phone Test

Install over v0.5.1. Point 9 is not required.

## Primary target: CMP101

1. Open the same CMP101 page from native sections.
2. Test the top legacy actions in the same order as before.
3. Expected:
   - action 3 still works;
   - actions 1, 2 and 4 should now navigate if they use named/numeric frame JavaScript.
4. Open the legacy combo/select.
5. Choose an item that previously did nothing.
6. Expected:
   selected content opens in the current viewer.

## CMP141

Recheck the page with the abbreviations table:
- white legacy canvas remains;
- text stays readable;
- links/forms should navigate if they relied on missing frame targets.

## Regression

Verify:
- native 101/103/... list still works;
- Modern returns to the same volume;
- Classic fallback works;
- PDF opens;
- zoom presets work;
- Fit width works;
- PDF save works.

If one control still fails, record:
- section code/title;
- which icon/button number;
- selected combo item text;
- whether anything flashes/changes after the tap.
