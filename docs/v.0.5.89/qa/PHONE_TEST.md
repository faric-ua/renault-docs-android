## Phone acceptance prerequisites — v0.5.89 signed release

- Install **only** original stable-signed v0.5.89/build105 from https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.89-debug, SHA-256 `46a2edc531725ca142e5f2a8afd2f5ff93ddb4e3061c919ede40e769097236db`.
- Upgrade existing app in place; **NEVER uninstall, clear storage, or delete installed tomes**.
- Distinguish old v0.5.87 device evidence from v0.5.89 regression evidence.
- Check project name, existing tome count/list and Classic availability before doing any operation.
- Use only **new, intended archives**. Old matching volumes should not be repeatedly imported to inflate notification count.
- During extraction/copy/staging/pack, record file-count stage stability (UX #100) and check no duplicate foreground notification.
- After two *real* new volumes, verify both completed results independently visible and group expandable, older result taps correct original project, dismiss one without dismissing all (UX #102).
- If notification access disabled by Android or vendor, note settings rather than claiming app pass/fail.
- Background lock/rotation #40 can be exercised safely during a legitimately needed conversion; don't force-stop/delete data.
- For #51, ZIP already confirmed for NT8298A; do not claim 7Z/RAR/batch/cancel PASS absent actual operation evidence.

Phone status: **PENDING, not executed with v0.5.89 at time of publication**.

---

# Notification history — phone QA pending

On verified stable-signed APK **installed over** the current application (no uninstall/clear-data):

1. In the phone notification shade, note any existing completion notifications; legacy fixed-ID history cannot be recovered retroactively.
2. Complete a planned NEW volume #1; leave its notification in place.
3. Complete a planned NEW volume #2; open the grouped Renault Docs results and confirm both NT/date labels are present, without the first disappearing.
4. Tap the first one to check it goes to its owning project; return and dismiss one child.
5. Observe live progress during one nonduplicate preparation: exactly one foreground progress notification and no per-file spam.
6. If the user has ten or more legitimate future imports, check ring boundedness; **do not import duplicates just to create ten notifications**.
7. For combined ZIP batch and RDPKG import, validate only when an actual safe user operation is planned.

CI/build PASS does not mean phone notification PASS. Android notification permission, notification grouping and vendor skin can affect appearance.
