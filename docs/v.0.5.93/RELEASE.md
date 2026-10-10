# v0.5.93 — history notifications after in-place APK update

User report, 2026-10-10: previous completed-volume notifications disappeared from Android shade after installing v0.5.92/build108 on top of the previous version. Installed volumes were not reported missing.

## Source audit
- Older `CompletedNotificationHistory.kt` preserved only each event's ID and a rotating 10-slot sequence in app-private preferences. Title, message and owning project were **not** persisted.
- Android may remove existing notifications during package replacement; a live shade notification is not persistent operation history. Consequently previously lost pre-v0.5.93 entries **cannot** be reconstructed from those preferences.
- No evidence source #115 explicitly invoked `cancelAll()` or erased history; do not assume an Android package update always behaves identically across devices.

## Candidate
Persist last 10 real terminal result records with sequence/event key, owning project, exact title/body and a dismissed flag. Add a private notification delete receiver so manual dismissal/clearing the group isn't undone. On an Activity resume after a future version change, restore only previously saved, non-dismissed records whose notification IDs are missing from current Android shade. Avoid repeated restore in same app version, avoid replay of running progress, and never reprocess source archives. Persisted ring still bounds retained records to 10.

**Migration limitation:** at the very first installation of v0.5.93, pre-0.5.93 notification IDs still lack their text. The upgrade initializes the new baseline, but can only restore **newly recorded** results on subsequent updates. No promises about recovering the two old messages that vanished installing 0.5.92.

Android can still hide/dismiss notifications, reject permissions or apply system grouping. Persistent **in-app** History would be a separate UX feature; app-private journal here exists only to improve upgrade resilience.

## Safety and acceptance
No ZIP/7Z/RAR, SAF output, mounted tomes or dataset identity is touched by this source change. Source tests and Kotlin tests precede any verified stable signed APK. Phone acceptance needs 2 new legitimate results to populate v0.5.93 journal, then a **future** update with retained data to validate restoration. No redundant importing or uninstalling to force this test.

Unrelated newly reported batch source issue #118: outer archive containing three inner archives with `index.html` inside — requires confirmation whether entries are nested compressed files vs ordinary folders. This candidate does not implement nested archive extraction.
