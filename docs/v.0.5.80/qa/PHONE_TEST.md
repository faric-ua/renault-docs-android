# v0.5.80 Phone QA — IN PROGRESS

- [x] Create-from-archive picker shows prepared `.rdpkg` entries **greyed out / disabled**; user confirmed they cannot be selected. PASS on phone (2026-10-08). No claim about providers that might still expose a selectable `.rdpkg`.
- [ ] Select original KangooII .zip while in Megane II: model mismatch rejection.
- [ ] Select genuine Megane .zip: preview shows filename, Document ID/provider, Megane target and other registered-project possible NT matches.
- [ ] Cancel preview: no run; previous diagnostic record remains.
- [ ] Rotate during preview: restore same exact source, no auto-start.
- [ ] Continue valid source: choose destination, normal status/progress and file result.
- [ ] Original source archives, existing packages and tomes remain intact.
- [ ] ZIP with generated renault-dataset.json marker refused before extraction.

## Phone observations

- 2026-10-08 — PASS (test 1): From the archive-source selector, prepared `.rdpkg` files remain visible but appear grey and cannot be chosen. Reporter: user on device. No conversion was requested as part of this test.
- Next isolated test: while in Megane II, choose an original Kangoo II `.zip`; expect explicit model-conflict rejection **before** destination picker or work begins. Leave all source files unchanged.
