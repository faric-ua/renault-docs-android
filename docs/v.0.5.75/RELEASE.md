# Renault Docs v0.5.75 — sticky Add panel and pin refinement

Status: **DEVELOPMENT**

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
