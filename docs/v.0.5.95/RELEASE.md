# v0.5.95 / build 111 — Native batch diagnostics

Fix #123 without modifying the accepted v0.5.94 nested-archive intake and native conversion.

- Record after each successful package commit a bounded per-volume entry (document label, package ID, volume ID, SHA-256, output SAF URI, section count); persist JSON with explicit schema version in the existing private native run preferences. Track skipped installed duplicates independently. Resume/recreate reads entries; failure and cancellation preserve entries. New start clears stale batch metadata.
- Multi-volume COMPLETE no longer claims last tomo's package ID as the identifier of all tomes. The read-only diagnostic shows each individual result, its hash and output URI; skipped volumes appear separately. Old v0.5.94 multi-volume records cannot be reconstructed: show `Дані про всі пакети в цьому старому звіті недоступні`, not fake hashes.
- Preserve single-package scalar diagnostics and terminal green card/progress without adding large hashes to primary status.
- Bounded SharedPreferences records (max 128 per category), explicit omitted counts; long reports scroll and are copyable using the existing dialog.
- Do not manipulate existing 20 Megane II tomes, archive input, SAF outputs, original hashes, stored package IDs, app signer, or background worker logic.

Phone QA pending: install above existing app; verify old legacy report stays readable, then during next actual new multi-volume processing check individual entries and copy. Never rerun existing installed files solely to test. Issues #40/#51/#102 remain open for separate background/archive/notifications verification.
