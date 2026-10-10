# v0.5.92 open QA
- #51: multi-root chooser and batch already exist; only ZIP single-volume pipeline has sustained phone evidence. Full mixed-root ZIP/7Z/RAR and partial success recovery are still not phone accepted.
- #40: seven background FGS + partial wake locks have source coverage, but real device screen-off/lock/unlock/foreground timeout constraints are still untested.
- New source issue: native Cancel during IMPORTING silently ignored because ACTION_CANCEL accepted PREPARING alone. Candidate supports cooperative cancellation inside RDPKG extraction, before atomic activation. QA pending.
- Batch partial completion notifications were only published on all-success terminal; a later cancellation/error hid intermediate successful results. Candidate now publishes immediately after each per-volume commit, without duplicate final publication. QA pending.
