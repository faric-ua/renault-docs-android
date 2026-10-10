# Renault Docs v0.5.96 — Android 15+ dataSync foreground service budget

## Device evidence (prior v0.5.95)
User tested real native NT8445 (2007-11-19, 333 native): lock screen about 2 minutes, switch to other apps including TikTok, expanded shade progress remained active; native result completed. PASS for ordinary lock/background, **not** process death or six-hour quota.

## OS contract (Android SDK 35+; current target 36)
Android 15+ `dataSync` FGS allowance is 6 hours total in a rolling 24-hour window shared across **all** dataSync workers of the app. At limit system calls `Service.onTimeout(startId, fgsType)` and expects `stopSelf()` within seconds; otherwise it can throw RemoteServiceException. Once quota exhausted, background-restarting another dataSync FGS may be disallowed. Foreground user interaction can reset quota.

Source: https://developer.android.com/develop/background-work/services/fgs/timeout

## Source changes
- All seven long-running Renault Docs services now override `onTimeout(Int, Int)`: signal shared atomic `DataSyncTimeoutGate`, persist a user-readable FAILED terminal reason **only if still running**, release held CPU lock, remove active progress notification and call `stopSelf()` immediately in `finally`. No new foreground work or delayed timer in callback.
- Worker callbacks check timeout before progress/terminal success. Native engine/archive & RDPKG importer cooperate using cancellation callbacks in ZIP loops; catalog download/import checks before each item/registration. Legacy conversion and RDPKG/RDPROJECT export/share check progress boundaries, but an uninterruptible blocking provider read could persist briefly until Android tears down the service.
- No arbitrary restart/retry after quota expiration, **no automatic deletion of already committed volumes**, no changes to user-owned archives or installed data. Partial staged files that are owned by existing converters are cleaned on their usual cancellation/error path as best effort; OS process termination is not a general rollback guarantee.

## Honest limits
- This is a **quota stop**, not automatic resume: after the app returns to foreground and the quota resets, users should inspect reported results before explicitly retrying an unfinished operation.
- `PARTIAL_WAKE_LOCK` is separately capped at six hours. Foreground service priority and wake lock improve behavior; neither defeats Android power policy, task force-stop/reboot, FGS quota or OS process death.
- A race with atomic on-disk commit cannot be perfectly unwound by a stop callback; existing completed packages are preserved. Run-store marks the work interrupted, not falsely complete. Do not claim crash-free guarantee on untested devices.
- The previously observed 20 Megane II volumes must not be erased/re-imported to simulate timeout.

## Gates
Python source contracts + Kotlin `DataSyncTimeoutGateTest`, Android PR CI; trusted signed main build; control test with Android 15+ **emulator** using Android official short `device_config` timeout; device owner need not perform six-hour stress. Emulator test must be separately labeled PENDING until actually run — CI unit tests are not an emulator.
