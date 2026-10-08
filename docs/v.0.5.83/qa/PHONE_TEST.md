# v0.5.83 phone checklist — PAUSED / SIGNED APK READY

When the user chooses to resume QA (not before):
1. Home `Додати` → `Новий том` → choose Megane II. Expect expanded project `Додати` (Auto, Manual, raw, archive) and **no SAF picker yet**.
2. Android Back goes back safely, no imports started and no data change.
3. Rotate on options screen and return: expanded state available, no automatic picker/conversion.
4. Deliberately tap `Авто` to verify explicit .rdpkg picker only if user has agreed to run the test; cancel it. Leave existing packages untouched.
5. Check that an intentional direct-picker route still exists for separate callers (code tests cover this).
6. Confirm project and volume counters unchanged.

Never launch a conversion, archive import, cleanup or file move solely to test this release. User requested a pause on manual tests on 2026-10-09.
\nSigned Android Debug APK #146 PASS; artifact ID 11582942367. No phone QA requested or performed for v0.5.83. No need to run the checklist until user chooses.\n