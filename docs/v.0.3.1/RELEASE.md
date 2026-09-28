# Renault Docs v0.3.1 — Fast Pack

## Real-phone finding

v0.3.0 confirmed that PDF export works, but the user reported that overall documentation navigation is still slow.

The reason is architectural: after the native Modern volume list, each legacy volume still loads many tiny HTM/JS/GIF files through Android SAF.

## Fast Pack

The package builder now creates:

`_renault/fast-content-<sha16>.zip`

It contains web resources only:
- HTM/HTML;
- JS/CSS;
- GIF/ICO;
- PNG/JPG/SVG;
- generated JSON.

PDF files are intentionally excluded because they already use the native PDF layer and can be large.

The manifest records:
- format;
- path;
- SHA-256;
- byte size;
- file count.

## Android runtime

When a dataset is opened:
1. Modern mode prewarms the Fast Pack in a background thread.
2. The archive is copied once from SAF into app cache and SHA-256 verified.
3. Legacy WebView resource requests check the local ZIP before SAF.
4. PDF requests still go through the native PDF layer.
5. If the Fast Pack is absent or unavailable, SAF remains a compatibility fallback.

This removes the repeated Storage Access Framework lookup/open cost for tens of thousands of small legacy web files.

## Compatibility

Classic mode remains available.
Existing datasets without Fast Pack still work through SAF.
Rebuilding package metadata is enough; the original conversion does not have to be repeated.
