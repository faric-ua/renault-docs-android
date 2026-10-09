# v0.5.87 — archive native source entrypoint

**ARCH-NT8298A-REAL-001 — DEVICE FAIL / ROOT CAUSE NOT FULLY PROVEN.** v0.5.86 Test3: `ARCHIVE_FILE`, Megane II, source ZIP 72,693,952 bytes; user screenshot of nested `INDEX.HTM` with `COMMUN` and `RUS`; native `stageScan` said no directly-rooted INDEX; no resulting package ID/SHA256. Existing diagnostic does not record selected extracted raw root. No failed user's output archives were modified or deleted by us.

**ARCH-NATIVE-HANDOFF-001 — SAFE RECOVERY IMPLEMENTED / PHONE PENDING.** Local native staging now resolves one unique nested raw root if handed a wrapper, refuses multiple/zero raw roots. The selected batch-candidate path is never silently replaced when there are excluded nested volumes.

**ARCH-DIAG-001 — MORE ACTIONABLE FAILURE.** If the index is missing from the actual native scan, show limited relative top-level filenames and index matches without personal SAF URI, enabling next decisive fix rather than repeatedly asking for a ZIP. No claim that this already fixes the user's exact 72.7 MB archive.

**Remaining #51:** real NT8298A acceptance, ZIP/7Z/RAR variation, cancel/staging cleanup, and phone lock/background (also #40).

## Signed candidate #150 READY for one real ZIP test

PR #98 merged source `937ffa06080c3dd6a63a87d4e9c5d209ff4b8755`; Python #591, Android #467, main #592, signed APK #150 PASS. Public debug prerelease v0.5.87-debug published by verified workflow 37938990631 PASS, unchanged signed APK SHA256 `60795a057ad77b96df0f914a8c9341222d9684fc0087e89d9389c558761286f5`. This **does not close** `ARCH-NT8298A-REAL-001`: waiting on original ZIP device acceptance or new bounded diagnostic. No need for further engineering before user's single test, absent new evidence.

## NT8298A real-device regression acceptance — PHONE PASS (2026-10-09)

User reported `COMPLETE` after retrying the same original ZIP, `ARCHIVE_FILE`, 319 native sections, volume `NT8298A · 2005-11-28`, Package ID `megane-ii-nt8298a-2005-11-28`, result package SHA256 `e7fdbea2d3363af3ea3710eda22dcee518e36d08963b3483f0610a55602f6603`, 119 seconds. **ARCH-NT8298A-REAL-001 is resolved/phone-confirmed for this exact real ZIP**. No independent per-page viewing or post-run project count provided. #51 remains open for other formats and batch/cancel/lock paths.

## UX-PROGRESS-JUMP — recorded as issue #100 (NO CHANGE)

During successful native .rdpkg creation, visible live status row shifts as stage text, processed-file counts and plain counts alternate. Potential source: dual plain and structured updates + variable TextView line wrapping. Root cause is not confirmed by video. User specifically asked to register and not fix yet. Issue: https://github.com/faric-ua/renault-docs-android/issues/100 . No code, APK, or UI change.

## Read-only deletion-scope audit

See `qa/DELETE_BEHAVIOR_READONLY.md`. Megane II and Kangoo II separate project prefixes for newly generated package IDs, while project remove only deletes the matching association (may invalidate project-share cache). The prepared share .rdpkg delete command may remove both canonical and legacy **private copies** for one volume; Android app clear-data affects the full app. No actual removal attempted.
