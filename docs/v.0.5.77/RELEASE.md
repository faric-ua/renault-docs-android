# Renault Docs v0.5.77 — orientation-aware Add panel

Status: PHONE PASS 6/6 / CLOSED — 2026-10-08

- versionName 0.5.77, versionCode 93.
- PR #77 merged, runtime source `87ab5c2dc20fa14dd7e88ac8cfb81d746c262169`.
- Tests #552 PASS, Android PR Check #442 PASS, main Tests #555 PASS.
- Android Debug APK #140 PASS; artifact `Renault-Docs-v0.5.77-Debug`, id `11523858486`, sha256 `ea2412820e0111797db40ae42b96e40e007fa3d0071ffec96348a991a9078694`.
- Portrait pin preference and expansion persist; landscape auto-collapses and disables pin without resetting portrait preference.
- Landscape manual expand/collapse allowed. Volume ScrollView fixed structure and 10dp gap unchanged.
- Original full-color/grayscale 3D emoji unchanged; status card design deferred.

## Real-phone acceptance

Phone acceptance received 2026-10-08 from user: PASS 6/6, comment «Все норм». Checked:
1. Portrait: Add expanded, pin red — PASS.
2. Landscape rotation: Add auto-collapses, pin grayscale — PASS.
3. Landscape: volume list scrolls freely — PASS.
4. Landscape: manual expand/collapse — PASS.
5. Return portrait: pinned red emoji and expanded Add return — PASS.
6. Reopen Megane II: persistent pinned state — PASS.

Scope: six explicitly reported gates only; separate unpinned rotation, repeated rotations, no auto-action and status-card behavior were not individually asserted in this six-item result.

Result: **ACCEPTED** for the six reported checks. No APK rebuild or new runtime SHA needed. The operation/status-card visual redesign is deferred to a separate task.
