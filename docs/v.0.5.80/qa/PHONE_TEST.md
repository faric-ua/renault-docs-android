# v0.5.80 Phone QA — IN PROGRESS

- [x] Create-from-archive picker shows prepared `.rdpkg` entries **greyed out / disabled**; user confirmed they cannot be selected. PASS on phone (2026-10-08). No claim about providers that might still expose a selectable `.rdpkg`.
- [x] Select original KangooII .zip while in Megane II: explicit model mismatch rejection before destination or preparation. PASS on phone (2026-10-08), screenshot shows «Неправильне джерело», «Джерело містить назву моделі kangoo, але вибрано проєкт «Megane II». Підготовку зупинено.»
- [x] Select genuine Megane .zip: preview shows filename, Document ID/provider, Megane target, and existing current-project NT metadata match. **PARTIAL PASS**: cross-project result not yet observed/tested.
- [ ] Cancel preview: no run; previous diagnostic record remains. (Not explicitly reported yet.)
- [x] Rotate during preview: same source preview remains without reset. PASS on phone (2026-10-08). No auto-start was reported.
- [ ] Continue valid source: choose destination, normal status/progress and file result.
- [ ] Original source archives, existing packages and tomes remain intact.
- [ ] ZIP with generated renault-dataset.json marker refused before extraction.

## Phone observations

- 2026-10-08 — PASS (test 1): From the archive-source selector, prepared `.rdpkg` files remain visible but appear grey and cannot be chosen. Reporter: user on device. No conversion was requested as part of this test.
- 2026-10-08 — PASS (test 2): User screenshot of Megane II displayed modal «Неправильне джерело»: Kangoo model detected and preparation stopped. No destination picker or generated package observed. Screenshot has soft keyboard still visible under modal; record as UX observation only, not a confirmed functional regression.
- 2026-10-08 — PARTIAL PASS (test 3): screenshot of «Підтвердь джерело архіву» for `Megane II B,C,S 84 Europe_NT8341A_Visu v3.0_2006.10.09.zip` under `primary:Documents/Renault/Megane II/Backup/`; provider `com.android.externalstorage.documents`, destination Megane II; preview reports installed `Megane II (поточний): NT8341A · 2006-10-09 — збіг метаданих`. This is not a hash match, and another-project results are not yet proven. User favors a warning with an option to continue intentionally.
- UX observation (not a blocker): preflight dialog is text-heavy with raw provider/Document ID prominent and keyboard visible underneath. Possible future improvement: concise duplicate summary, details expandable, preserve explicit Continue/Cancel. No UI code change authorized or performed.
- Next isolated test while this very preview is open: rotate screen; ensure same source preview persists with no automatic conversion. Then Cancel; check no run. Do not click Continue or choose output.

- 2026-10-08 — PASS (test 4 rotation): User confirms turning device leaves the archive source confirmation window in its previous state. Cancel/no-run branch remains to be confirmed separately.
