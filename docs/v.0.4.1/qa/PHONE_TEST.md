# v0.4.1 Phone Test

Install over v0.4.0.

## A. Classic → Modern
1. Settings → default dataset mode = Classic.
2. Open Laguna from Library.
3. Confirm top viewer header contains:
   - Modern;
   - Home;
   - Search;
   - Settings.
4. Tap Modern.
5. Expected:
   - Modern opens without returning to Library first;
   - same dataset is open.
6. Open a specific volume in Modern.
7. In the viewer tap Modern again.
8. Expected:
   - same volume card is highlighted and scrolled into view.

## B. Home / Settings
1. From viewer tap Home.
2. Expected: return to Renault Docs Library.
3. Open a document again.
4. Tap Settings.
5. Expected: Settings opens and Back returns to document context.

## C. Search on current HTML page
1. Open a legacy HTML page containing visible text.
2. Tap Search.
3. Enter a known word.
4. Expected: match counter appears and WebView highlights matches.
5. Test previous/next.
6. Close search.
7. Rotate while search is open.
8. Expected: search row and query survive rotation; document is not re-launched from scratch.

PDF note:
current PDF pages are raster images, so text inside the PDF image is not searchable in this release.

## D. PDF toolbar
1. Open a PDF.
2. Confirm every button label/glyph is centered.
3. Confirm field text `100%` is centered.
4. Confirm `↔ По ширині` is visible and centered.
5. Confirm +/- and presets still work.
6. Confirm Save PDF still works.
7. On a narrow screen, if the full toolbar does not fit, it should scroll horizontally rather than crop controls.
