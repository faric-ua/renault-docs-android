# Phone QA — v0.5.96 (PENDING)

Install only the trusted stable-signed main APK *over* Renault Docs without uninstalling/clearing data. Check current registered tomes first; no destructive conversion solely to test.

During the next legitimate operation, briefly lock the screen, switch between apps, expand the Android shade progress and return to Renault Docs. A normal operation should continue and finish; this confirms no regression to v0.5.95 ordinary background behavior.

Do not wait 6 hours, alter `device_config`, force stop, reboot, disable Samsung battery protections or cause artificial source failures. System quota handling is a separate controlled emulator QA item.

Report the source run, duration, terminal state and any stuck progress/duplicate notifications. Retain #40 OPEN for emulator quota and other six-worker QA.
