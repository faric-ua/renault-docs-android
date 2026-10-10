# Android 15+ short dataSync timeout test — controlled emulator ONLY

Status: **NOT RUN**. Do not run these shell commands on the user's actual handset; the commands alter a device-wide setting. Use a clean throwaway Android 15/16 AVD with a *test copy of the project* and test data, not the user's SAF folder.

Official testing instructions: https://developer.android.com/develop/background-work/services/fgs/timeout

1. Start emulator API 35+ with a debug/test build, `adb devices` to confirm serial. Put test app in background while a disposable native/export action is active.
2. Record existing value with `adb -s EMULATOR_SERIAL shell device_config get activity_manager data_sync_fgs_timeout_duration`. Record whether the value is null. Run `adb -s EMULATOR_SERIAL shell am compat enable FGS_INTRODUCE_TIME_LIMITS com.saney.renaultdocs` if the emulator requires the compatibility switch.
3. On *emulator only*, shorten quota: `adb -s EMULATOR_SERIAL shell device_config put activity_manager data_sync_fgs_timeout_duration 60000`. Restart disposable workload and send test app to background.
4. Watch `adb -s EMULATOR_SERIAL logcat -s ActivityManager AndroidRuntime RenaultDocs`. Expect onTimeout, worker phase FAILED with readable quota reason, active FGS removed, no `RemoteServiceException: ... did not stop within its timeout`, and no new fake COMPLETE. Already committed packages must remain.
5. Reset the device-wide setting to its exact prior value: if previously `null`, `adb -s EMULATOR_SERIAL shell device_config delete activity_manager data_sync_fgs_timeout_duration`; otherwise `adb -s EMULATOR_SERIAL shell device_config put activity_manager data_sync_fgs_timeout_duration PREVIOUS_VALUE`. Disable the temporary compat flag on this disposable emulator if enabled by this test. Prefer destroying the throwaway AVD.
6. Independently verify catalog/import/export/share jobs as sample data becomes available; one emulator native test does not prove all seven integrations.

**Do not report emulator PASS unless an actual ADB/emulator run and log evidence have been produced.** Kotlin unit tests simulate the gate's state, not the system callback.
