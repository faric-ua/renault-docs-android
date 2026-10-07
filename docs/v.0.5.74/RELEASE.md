# Renault Docs v0.5.74 — Add panel status/pin/chevron refinement

Status: **MERGED / MAIN CI PASS / PHONE QA PENDING**

## Phone evidence from v0.5.73

The compact Add layout itself is visually successful:
- one full-width `Додати` header is visible when collapsed;
- expanding reveals the existing Auto / Manual / raw / archive actions;
- the volume list moves upward when collapsed.

New phone UX findings:
1. transient text such as `Імпорт .rdpkg скасовано.` appears as bare text between Add and the volume list;
2. the pin uses a colored emoji, so its inactive/active state is not visually controlled;
3. the expand/collapse chevron is too light;
4. the permanent tap/long-press guidance is no longer useful inside the expanded Add panel.

## v0.5.74 target

- move transient add/import status into a small warm-toned status card at the bottom of the expanded Add panel;
- hide that card when status is blank;
- remove the tap/long-press guidance text;
- replace emoji pin with a monochrome vector:
  - inactive = neutral gray/white;
  - pinned = red;
- use a heavier `▲ / ▼` expand/collapse indicator;
- keep active operation/progress status outside the collapsible body so running work is never hidden.

No add/import workflow semantics change.


## CI evidence

- PR #72 merged.
- Runtime source: `8dc7a3d0c6d22c50df343a36456f6bd4716b3a5a`.
- Tests #542: PASS.
- Android Debug APK #137: PASS.
- Artifact: `Renault-Docs-v0.5.74-Debug`.
- Artifact ID: `11518058878`.
- Digest: `sha256:a391f4c16a0ce7f3c7b79a9fd9383e1e0e386f07e7d5268e1b4938eaf657f230`.

Phone QA remains required.
