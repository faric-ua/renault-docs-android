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
