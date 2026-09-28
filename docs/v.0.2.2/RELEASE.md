# Renault Docs v0.2.2 — Android PDF + performance

## Phone finding from v0.2.1

The SAF-backed HTML viewer works on the real phone:
- dataset opens;
- 10 Laguna volumes are visible;
- legacy Renault frames/menu/images render;
- PDF navigation reaches the controlled placeholder.

The user also reported that the app feels noticeably slow.

## Root cause under investigation

The first SAF bridge resolves every HTML/JS/GIF/frame resource with repeated `DocumentFile.findFile()` calls along the full path. On a legacy corpus with many files and many resources per page, this creates many expensive Storage Access Framework provider queries.

## Goals

- add a lazy SAF directory/file cache so each directory is enumerated once and reused;
- keep exact path semantics, with safe unique case-insensitive fallback;
- switch WebView from forced no-cache to normal local caching;
- replace the PDF placeholder with an Android-native PDF layer using `PdfRenderer`;
- keep PDF inside the same legacy WebView/frame;
- render pages lazily near the viewport;
- continuous vertical page scroll;
- page counter + previous/next + zoom controls;
- bounded rendered-page byte cache;
- no Internet permission and no external PDF app dependency.

## Known limitation

Exact Adobe `#viewrect` semantics are not claimed in v0.2.2. The original fragment remains part of the page URL, but exact coordinate parity is deferred until separately validated.
