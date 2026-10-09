# v0.5.86 phone QA — DEFERRED at user's request

No device test and no app installation requested automatically.

Future safe test: create/choose controlled mixed-root archive containing a root raw volume (INDEX.HTM), a nested independent NT volume (INDEX.HTM), and a normal shared asset. Confirm chooser lists both; creating root produces only root's assets/entries and no nested independent volume; creating the nested one produces exactly its own files. Confirm package identity, duplicate prevention, source archive preservation, temporary staging cleanup, rotation/lock, and explicit cancellation behavior. Repeat with other supported formats when sample archives are available. Observe file results; do not mark PASS based on compilation alone.

#40 Android background quota/timeout is still separately open. No force-stop/reboot guarantees.


## Release readiness — 2026-10-09

Stable signed debug APK #149 PASS and public verified prerelease https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.86-debug are available. **No v0.5.86 installation or on-device archive import took place.** Previous v0.5.84 was phone-accepted only for over-install/data retention; do not ascribe this result to v0.5.86. Extended phone QA remains paused, issue #51 OPEN.

## Phone Test 1 — installation/data retention: USER PASS (2026-10-09)

User replied «Пасс» directly to the requested 4-step live check: Renault Termux `5 → 19 → 8 → 13`, install v0.5.86/build102 over the existing app without uninstall/clear data, launch Renault Docs, and verify registered automobiles and volumes are still present. **PHONE PASS for installation + app launch + retention only**. Per-volume counts were not independently reported. ZIP/7Z/RAR actual intake, parent/child .rdpkg package content, cancel/lock/background and source staging are NOT verified yet.

## Next live test: archive source confirmation (PENDING)

Open a project matching an existing Renault ZIP/7Z/RAR → `Додати` → `Створити .rdpkg з архіву` → choose the archive with SAF. Expect the dialog `Підтвердь джерело архіву` showing the correct archive filename/project and duplicate-warning text. **Do not press Continue/choose destination yet**. Send screenshot; no native conversion should have started. This is an input/preflight test, not a successful ZIP/7Z/RAR import claim.
