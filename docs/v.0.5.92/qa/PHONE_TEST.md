# Safe phone QA — v0.5.92 PENDING

Only use a stable-signed main APK, installed over existing Renault Docs without uninstall or data clearing. User-approved new archive operations only.

## A. Multi-volume batch (issue #51)
1. Choose **one** ZIP/7Z/RAR archive with at least two independently valid Renault raw roots. Confirm preflight root count and explicit chooser; verify candidate labels/dedup indicators. Do not modify the source archive.
2. Select two *new* volumes only and confirm destination SAF folder. Confirm no hidden automatic selection and that outputs are separate `.rdpkg` files.
3. Each completed volume should immediately create its **own** grouped ready notification, even while another remains processing. No notification per individual ZIP file.
4. Do not trigger failures by damaging any archives. If natural failure/cancel occurs, verify already-completed volumes remain installed, unfinished output is cleaned, and source archives untouched.
5. Verify independently selected parent/child roots do not embed one another; compare IDs and volume counts without deleting any files.
6. Encrypted/corrupt or unsupported files should give a clear reason when genuinely encountered. Do not generate malicious archives merely for phone testing.

## B. Background / lock (issue #40)
1. During an actually intended conversion, allow screen off, lock phone for a few minutes, return: one active foreground notification and the same run/project, progress should have advanced or completed.
2. Open another app while working; returning to Renault Docs must reattach to persisted progress, not restart or duplicate installed tomes.
3. During a future real multi-volume job, try rotating while chooser is open (before conversion) and while worker is processing (screen orientation only; don't force stop).
4. If cancellation is actually needed during verification/importing, it should be acknowledged. Already committed volumes must be preserved; incomplete staged output removed.
5. Note Android battery/background restrictions and OS dataSync quota. **Do not** force-stop, reboot, clear app data or try a six-hour timeout for routine QA.

Record observed PASS/FAIL separately for ZIP, 7Z, RAR, batch, lock, restore, cancel, and terminal cleanup. Do not close #40/#51 on automated CI alone.
