# Renault Docs v0.5.69 — Archive Intake

Status: **MERGED / MAIN CI PASS / PHONE QA PENDING**

## Goal

Import old Renault documentation archives directly from Android SAF without manual unpacking:

`ZIP / 7Z / RAR → private safe staging → Renault raw detection → existing native raw→.rdpkg engine → validation/install`.

## Merged behavior

- source archive is read-only and remains unchanged;
- extraction is confined to app-private staging;
- path traversal is rejected;
- file-count / expanded-size / free-space guards apply;
- archive listing can detect obvious exact duplicates before full extraction;
- one discovered new volume can continue directly;
- multiple volumes enter persisted `WAITING_SELECTION`;
- already-installed volumes are disabled/unselected;
- user can select one or several new volumes;
- selected volumes continue from existing extracted staging;
- every candidate is duplicate-checked again before conversion/import;
- operation uses the existing foreground service, wake lock, progress and Cancel contract;
- stale partial batch output is cleaned after process loss;
- each selected volume receives a canonical .rdpkg in the explicit destination folder;
- private staging is cleaned on success/cancel/failure.

## Merge evidence

- PR #53 merged;
- feature head: `f9f11e4a3770b4d098a134217f32c036cfe21613`;
- main: `53f4208b4b2e4d27e4bb55094c855eaf13fb2442`;
- PR Tests #525 PASS;
- Android PR Check #423 PASS;
- main Tests #526 PASS;
- Android Debug APK #132 PASS;
- artifact: `Renault-Docs-v0.5.69-Debug` / id `11489564391`.

## Phone gate

The first phone test is deliberately **ZIP**. After the ZIP path is accepted, cover duplicate and multi-volume behavior, then lifecycle/cancel/background gates. Issue #51 stays open until phone PASS.
