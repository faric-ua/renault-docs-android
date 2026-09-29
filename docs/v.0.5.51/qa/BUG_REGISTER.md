# v0.5.51 Bug Register

## Closed during phone validation

### OOM during Runtime IR serialization — CLOSED
Symptom:
Android failed while allocating a large JSON/string buffer during native preparation.

Resolution:
Runtime IR output moved to buffered recursive streaming instead of whole-document `toString()/writeText`.

Verification:
subsequent real NT8340A runs completed.

### Parent/mixed folder selected as one volume — CLOSED
Symptom:
a parent Megane II folder staged 21880 files and then failed because two Renault volumes were discovered.

Resolution:
fail fast when the selected source is not exactly one Renault volume.

Verification:
correct NT8340A source completed with 7653 source files.

### Destination created inside source tree — CLOSED
Symptom:
the output document could be selected inside the raw source and appear in recursive scanning.

Resolution:
reject destination-inside-source and clean the newly created empty destination.

### Stale Activity-result replay starts second conversion — CLOSED
Symptom:
after the first operation advanced/completed, UI could return to a fresh raw scan without an explicit new user start.

Resolution:
persisted picker-session UUID + durable one-shot request claim.

Phone verification:
- post-completion app/window handoffs did not restart conversion;
- active PREPARING continued one run `500/7653 → 1400/7653 → 2300/7653`.

## Open non-blocking UX findings

### Terminal status remains visible
Desired:
right-side dismiss `×` for COMPLETE / CANCELLED / FAILED only.

### Ukrainian singular wording
Current:
`Імпортую .rdpkg… 1 файлів`

Desired:
`Імпортую .rdpkg… 1 файл`

### Historical public `*_android` clutter
Observed:
- `laguna 2 2001-2006_android`;
- `Megane II_android`;
- `Megane II_NT8342A_android`.

Action:
read-only provenance/reference audit before any deletion.
