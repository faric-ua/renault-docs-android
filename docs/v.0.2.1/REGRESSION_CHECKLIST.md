# v0.2.1 Regression Checklist

- [ ] Library still shows registered Laguna dataset.
- [ ] Dataset still reports 10 volumes.
- [ ] Tap dataset opens `_renault/START.html`.
- [ ] Catalog renders 10 volume cards.
- [ ] Tap each representative volume opens its legacy `INDEX.HTM`.
- [ ] Legacy frames/resources load from SAF.
- [ ] JavaScript navigation works.
- [ ] WebView Back returns to previous documentation page.
- [ ] Back from catalog returns to Library.
- [ ] Rotation restores current WebView page/state without reopening Library/picker.
- [ ] External URLs are not silently loaded inside local origin.
- [ ] Missing local files show a readable error.
- [ ] PDF links show a clear temporary PDF-not-yet-supported page.
- [ ] No `INTERNET`, `MANAGE_EXTERNAL_STORAGE`, or broad storage permission added.
- [ ] Android unit tests pass.
- [ ] Debug APK CI passes.
