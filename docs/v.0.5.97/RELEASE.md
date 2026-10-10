# Renault Docs v0.5.97 / build113 — RAW_TREE cross-model safety

Status: MERGED / PR + MAIN CI PASS / STABLE SIGNED APK #160 PASS / PUBLIC PROMOTION PENDING / PHONE QA PENDING.

## Background
v0.5.80 already rejected already-prepared .rdpkg files and obvious Kangoo/Megane/Laguna source-name conflict on original ZIP/7Z/RAR archives, including late worker validation and extracted root checks. Preflight shows the exact Android SAF selected source and NT catalog hints. The pre-existing #82/#85 issues still need truthful implementation-vs-phone-acceptance tracking.

## Scoped change
- The separate **raw SAF folder** path in ProjectActivity now rejects *explicit* cross-model source folder names before opening the .rdpkg destination picker, using exactly the established ArchiveSourceGuard.conflictingModel heuristic.
- NativeRdpkgPreparationService.runRawPreparation independently checks the actual stored project ID and the request's raw source name **before** processPreparedSource. Prevents bypass through service restart/re-delivery/caller paths.
- A model-neutral raw folder such as NT8486, X61 or generic Renault retains existing behavior; no guessed vehicle identity from platform/NT codes. Explicit Kangoo under Megane II or Laguna II fails closed; matching Kangoo under Kangoo II remains allowed.
- Existing ZIP/7Z/RAR flow, output names, installed volumes, import/export, notifications, Classic, and raw compiler remain untouched. No automatic cleanup of the user's earlier misclassified package, original ZIP or files.

## Test & release gates
- Kotlin ArchiveSourceGuardTest extended for raw folder matching/mismatch/unknown IDs.
- Python source contract confirms UI check before destination selection and independent worker check before processing.
- Exact-head PR Python Tests + Android PR Check (Kotlin/compile) required.
- Review/merge after PASS only; trusted stable-signed main APK and matching SHA-256, verified public debug release as usual.
- Device smoke: install above v0.5.96 without clearing data; existing project + volumes preserved. Raw picker selecting existing explicitly mismatched Kangoo-named folder in Megane must show rejection **without choosing destination or starting conversion**, press OK; do not create/edit/delete/reimport anything. If the user has no safe mismatched test folder, defer this gate until natural use.
- The real original Kangoo source selection history (#83) remains unproven; raw guard does not retroactively explain it. #40 emulator OS quota remains untested; #133 access/licensing is future TODO.
