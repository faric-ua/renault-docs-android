# v0.4.2 Phone Test

Install over v0.4.1.

## A. Search layout
1. Open an HTML page.
2. Tap Search.
3. Enter `2002` or another visible query.
4. Expected:
   - full query remains visible;
   - match counter is on the same first row;
   - previous / next / close controls are on a second row.
5. Test previous and next.
6. Rotate with search open.
7. Expected: query and open search panel survive.

## B. Classic catalog chronology
1. Open the Classic package catalog with the 10 Laguna volumes.
2. Do not rebuild the dataset.
3. Expected order begins:
   - NT8183A · 2001-01-22
   - NT8218A · 2002-05-01
   - NT8236A · 2002-11-18
   - NT8240A · 2003-11-17
4. Continue and confirm years progress to 2006.
5. Open a card and verify navigation still works.

## C. PDF Fit width
1. Open a PDF.
2. Inspect `По ширині`.
3. Expected:
   - double-arrow icon is visually centered vertically;
   - icon + label are centered as one group;
   - button height matches neighboring controls.
4. Tap it after zooming to 150% or 200%.
5. Expected: PDF returns to fit-width and horizontal offset resets.

## D. Regression
Confirm:
- PDF save;
- +/- zoom;
- zoom presets;
- Fast Pack speed;
- Classic → Modern;
- Home;
- Settings.
