# v0.5.1 Phone Test

Install over v0.5.0. Point 9 is not required.

## A. Transparent legacy page
1. Open the same CMP141 page that previously showed black text on the app's dark background.
2. Expected:
   - page canvas becomes white;
   - black table/text becomes readable;
   - explicit blue Renault pages remain blue.

## B. Named-frame link compatibility
1. From native sections open pages that previously looked loaded but some links/buttons did nothing.
2. Try legacy links/forms that navigate to another internal page.
3. Expected:
   - links targeting a missing old frame navigate in the current viewer;
   - no blank external window is required.

## C. Legacy widgets
Verify:
- select/dropdown based pages still work;
- engine/image choices still work;
- PDF links still open the PDF viewer;
- Modern returns to the same native section list.

## D. Regression
- native section list still opens;
- Classic fallback still works;
- page Search still works;
- PDF zoom preset dropdown still works;
- Fit width still works;
- original PDF save still works.

Record exact section code/title for any control that still does nothing. Those remaining cases likely use direct parent.frames[...] JavaScript and need a more specific bridge.
