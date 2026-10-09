# Renault Docs v0.5.89 — completion notification history

Status: **DEVELOPMENT / stacked PR and phone QA pending**.

Device bug #102: a second completed volume replaced a previous one because native foreground and final status used the same fixed Android notification ID.

Implementation:
- Native creation and regular RDPKG import both retain a single live foreground notification (IDs 3702 and 3703).
- Successful and unsuccessful terminal results have their own grouped, swipeable notifications with 10 bounded rotating IDs 48001–48010 and a separate summary ID 48000.
- Multi-volume archive batches publish each installed volume separately.
- A shared preference-backed ring remembers emitted keys to prevent repeated notifications after process restart/redelivery; newer entries replace the oldest after ten.
- Tapping an individual result opens the original project through a per-result PendingIntent request code; it cannot be rewritten by another project's subsequent notification.
- Notifications are **not** a permanent operation history: Android/user can dismiss them, hide the notification channel or apply system grouping/limits; previous overwritten notifications cannot be restored.
- Notification failures cannot turn a successful package preparation into a failed operation.
- No change to ZIP/RDPKG content, SAF output, installed data, deleting/renaming archives, cancellation, Classic or Modern viewers.

A current foreground progress notification must not be posted for every file/progress tick; only terminal results enter the bounded history.

This feature is stacked on v0.5.88/PR #101: merge/check the lower progress UI PR before promoting to main. No phone result or APK has been validated yet.
