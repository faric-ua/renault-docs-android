# v0.5.87 phone acceptance — SINGLE ACTION AFTER VERIFIED RELEASE

**Do not ask for testing before signed APK is published.** Update Renault Docs in-place; do NOT uninstall, clear project data, move or unpack original archives.

Only one live check requested after install: Megane II → Add → Create .rdpkg from archive → pick the same NT8298A ZIP → confirm source → choose the intended export directory. Report the final Completed/Failed state and whether volume count changes (was 10 before test). If failed, send exact new bounded error including `файли верхнього рівня` and `знайдені INDEX`; no full SAF URI necessary. If succeeded, verify resulting package label and exactly one new volume; original file remains intact.

Success of synthetic ZIP tests and installation smoke must not be confused with this real-archive acceptance. Do not invent a PASS.

## Signed APK readiness — 2026-10-09

All code/CI delivery gates PASS: PR Python #591, Android PR #467, main Tests #592, stable signer APK #150 (run 37938581867), explicit immutable promotion #99 and verified release publisher run 37938990631. GitHub release: https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.87-debug; actual APK SHA256 `60795a057ad77b96df0f914a8c9341222d9684fc0087e89d9389c558761286f5`. **No v0.5.87 device test has yet been reported.** Await the user's one controlled real NT8298A ZIP retry after in-place installation. Existing source archives must remain unchanged.
