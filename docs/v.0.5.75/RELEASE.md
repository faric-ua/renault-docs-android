# Renault Docs v0.5.75 — sticky Add panel and pin refinement

Status: **MERGED / MAIN CI PASS / PHONE QA PENDING**

## Phone feedback from v0.5.74

Accepted:
- compact Add concept;
- red pinned state;
- heavier chevron.

Requested refinements:
- keep the Add panel fixed at the top while only volume cards scroll;
- add a small gap between Add and the first volume card;
- restore the previous diagonal push-pin silhouette;
- inactive pin should be monochrome/light, active pin should remain red;
- status-card appearance will be reviewed later and is not redesigned in this release.

## Implementation target

- title/count/Add stay outside the volume ScrollView;
- only the volume list scrolls;
- 10dp spacing separates the fixed Add/status area from the first volume card;
- pin uses one diagonal vector silhouette with tint:
  - unpinned = light monochrome;
  - pinned = red;
- Help text describes the pin state without a colored emoji.


## CI evidence

- PR #74 merged.
- Runtime source: `b25efbee254562d7b3e9f6917772e8d2e6370eb3`.
- Tests #547: PASS.
- Android Debug APK #138: PASS.
- Artifact: `Renault-Docs-v0.5.75-Debug`.
- Artifact ID: `11520145404`.
- Digest: `sha256:59888f89ef694512ffa2a4895a75cb93b21c99687ccaa24270bfa9a06f948f00`.

Phone QA remains required.
