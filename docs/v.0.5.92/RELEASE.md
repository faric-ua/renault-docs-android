# Renault Docs v0.5.92 — archive batch + background safety

## Scope
Continue from v0.5.91 phone-accepted native ZIP progress, notification history and terminal layout. Code increment focuses on:
- Support Cancel during native PREPARING **and IMPORTING**; the previous native Cancel action only handled PREPARING.
- Cooperatively cancel RDPKG extraction per ZIP entry and read buffer and once more after payload validation. The last cancellation checkpoint is **before** atomic backup/install rename, preserving rollback semantics. After commit begins, that short atomic step is not interrupted mid-rename.
- For multi-volume archive batch, post a separate notification as soon as **each volume** has been validated/imported and registered. Later failure/cancel does not hide already installed successful volumes. Deduplicate by project, run start and package ID; no file-by-file notifications and no duplicates when the whole batch completes.
- Preserve explicit multi-root chooser, duplicate guard, nested-root isolation and private temporary staging cleanup.
- Seven services already use foreground notifications and a bounded PARTIAL_WAKE_LOCK; add regression checks for all seven. **No claim** that static tests prove actual screen lock/unlock, force-stop, device reboot or Android dataSync time quota behavior.

## Safety
No original ZIP/7Z/RAR, shared output, pre-installed volume or installed app data is modified by source changes in this PR. Existing native pipeline still writes only to the user-approved SAF destination and private staging and preserves transactional installation rollback. Cancellation can leave earlier **successfully installed** batch volumes in place; do not silently delete them.

## Known limits
- Mixed-root batches, nonduplicate 7Z and RAR files, encrypted/corrupt archives and real background lifecycle remain PHONE QA PENDING.
- Android 15+ dataSync foreground services have a system-limited background execution budget; a PARTIAL_WAKE_LOCK is not permission for unlimited work.
- Force-stop/reboot are hard boundaries and do not promise automatic replay.
- Process death mid-batch may require rechecking persistent package/volume identity and partial summary details; this is not fully validated on a physical phone.
