# v0.5.84 phone QA — PAUSED

**Not to run without user's later explicit decision.** Plan a safe, non-destructive test for each service once available, with small prepared sample inputs and outputs, exact runs, foreground notification and wake timeout behavior; rotate, lock/unlock, background and return. Force-stop/reboot documented as hard boundaries; no false claim of restart across reboot.

No current v0.5.84 on-device evidence. Do not start conversions or imports just to populate test data.

## Publication state

v0.5.84/build100 signed Android debug APK #147 PASS and available through verified GitHub prerelease `v0.5.84-debug`. **This is not a phone-test PASS**; user explicitly paused live QA. No installation or conversion was triggered as part of the release.

## Installation / retention smoke — PHONE PASS (2026-10-09)

The user replied «Пасс» to the explicit sequence: install v0.5.84/build100 over the previous installed Renault Docs without uninstall/clear-data, open the app, and confirm registered projects and installed volumes remain present. **PHONE PASS specifically for installation / launch / project-volume retention** based on user confirmation. No individual project counts were re-reported, so do not claim independent per-volume verification. **Issue #40 background lifecycle lock/unlock/process-death/long-run operation QA remains PAUSED / NOT PASS.** No conversions or import were requested as part of this smoke.
