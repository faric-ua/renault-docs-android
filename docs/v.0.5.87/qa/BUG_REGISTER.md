# v0.5.87 — archive native source entrypoint

**ARCH-NT8298A-REAL-001 — DEVICE FAIL / ROOT CAUSE NOT FULLY PROVEN.** v0.5.86 Test3: `ARCHIVE_FILE`, Megane II, source ZIP 72,693,952 bytes; user screenshot of nested `INDEX.HTM` with `COMMUN` and `RUS`; native `stageScan` said no directly-rooted INDEX; no resulting package ID/SHA256. Existing diagnostic does not record selected extracted raw root. No failed user's output archives were modified or deleted by us.

**ARCH-NATIVE-HANDOFF-001 — SAFE RECOVERY IMPLEMENTED / PHONE PENDING.** Local native staging now resolves one unique nested raw root if handed a wrapper, refuses multiple/zero raw roots. The selected batch-candidate path is never silently replaced when there are excluded nested volumes.

**ARCH-DIAG-001 — MORE ACTIONABLE FAILURE.** If the index is missing from the actual native scan, show limited relative top-level filenames and index matches without personal SAF URI, enabling next decisive fix rather than repeatedly asking for a ZIP. No claim that this already fixes the user's exact 72.7 MB archive.

**Remaining #51:** real NT8298A acceptance, ZIP/7Z/RAR variation, cancel/staging cleanup, and phone lock/background (also #40).
