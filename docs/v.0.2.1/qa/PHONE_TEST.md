# v0.2.1 Phone Test

1. Open Renault Docs.
2. Tap the existing Laguna dataset tile.
3. Expected: catalog page appears, not placeholder text.
4. Confirm 10 volume cards are visible.
5. Open at least:
   - NT8183A;
   - NT8240A;
   - NT8328A.
6. Expected: each opens its legacy Renault page.
7. Navigate inside one volume and press Back.
8. Expected: WebView goes back within docs; from catalog Back returns to Library.
9. Rotate while inside a volume.
10. Expected: same documentation state remains; no picker/library reset.
11. Tap one PDF link.
12. Expected for v0.2.1: explicit temporary PDF message, not silent blank/error.

Do not mark PDF rendering PASS in this release.
