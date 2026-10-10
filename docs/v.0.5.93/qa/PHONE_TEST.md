# Notification upgrade recovery — device QA pending

1. Install trusted stable-signed v0.5.93 *over* existing app; no uninstall/clear data. Old vanished v0.5.92 notifications will not be recreated retroactively: previously only event keys were saved.
2. Check existing Megane II 17 tomes remain and no automatic conversion starts.
3. During the next legitimate new archive conversions, inspect grouped results and confirm distinct completed children show as before.
4. Deliberate dismissal is optional and not required for data QA; if user naturally dismisses a notification, it should never be resurrected by a later app version update.
5. After a future legitimate upgrade (not a forced reinstall), results saved by v0.5.93 should be restored if the OS removed them, but not duplicated if they remained active.
6. No lingering live foreground progress notification after terminal. No new process starts and no repeated import. Preserve own project on tapping old child.

Track phone evidence as **pending** until a subsequent version update exercises recovery. Source CI alone does not validate OS deletion semantics.
