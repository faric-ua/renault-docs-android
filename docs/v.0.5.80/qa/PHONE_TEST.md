# v0.5.80 Phone QA — IN PROGRESS

- [x] Create-from-archive picker shows prepared `.rdpkg` entries **greyed out / disabled**; user confirmed they cannot be selected. PASS on phone (2026-10-08). No claim about providers that might still expose a selectable `.rdpkg`.
- [x] Select original KangooII .zip while in Megane II: explicit model mismatch rejection before destination or preparation. PASS on phone (2026-10-08), screenshot shows «Неправильне джерело», «Джерело містить назву моделі kangoo, але вибрано проєкт «Megane II». Підготовку зупинено.»
- [ ] Select genuine Megane .zip: preview shows filename, Document ID/provider, Megane target and other registered-project possible NT matches.
- [ ] Cancel preview: no run; previous diagnostic record remains.
- [ ] Rotate during preview: restore same exact source, no auto-start.
- [ ] Continue valid source: choose destination, normal status/progress and file result.
- [ ] Original source archives, existing packages and tomes remain intact.
- [ ] ZIP with generated renault-dataset.json marker refused before extraction.

## Phone observations

- 2026-10-08 — PASS (test 1): From the archive-source selector, prepared `.rdpkg` files remain visible but appear grey and cannot be chosen. Reporter: user on device. No conversion was requested as part of this test.
- 2026-10-08 — PASS (test 2): User screenshot of Megane II displayed modal «Неправильне джерело»: Kangoo model detected and preparation stopped. No destination picker or generated package observed. Screenshot has soft keyboard still visible under modal; record as UX observation only, not a confirmed functional regression.
- Next isolated test: choose a genuine Megane II `.zip` in Megane II to inspect source-preflight and matches across registered projects. **Cancel** from preview, do not choose output or start conversion; confirm no operation triggered.
