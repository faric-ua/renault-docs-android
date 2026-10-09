# v0.5.86 phone QA — DEFERRED at user's request

No device test and no app installation requested automatically.

Future safe test: create/choose controlled mixed-root archive containing a root raw volume (INDEX.HTM), a nested independent NT volume (INDEX.HTM), and a normal shared asset. Confirm chooser lists both; creating root produces only root's assets/entries and no nested independent volume; creating the nested one produces exactly its own files. Confirm package identity, duplicate prevention, source archive preservation, temporary staging cleanup, rotation/lock, and explicit cancellation behavior. Repeat with other supported formats when sample archives are available. Observe file results; do not mark PASS based on compilation alone.

#40 Android background quota/timeout is still separately open. No force-stop/reboot guarantees.
