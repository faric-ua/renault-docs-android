# Renault Docs v0.5.31 — dedicated volume documentation screen

Date: 2026-09-26

## Goal

Make the UI ownership match the v0.5.29 data model:

- one Renault volume owns one documentation set;
- documentation is no longer conceptually hosted by section 101/103/105/etc.;
- sections can still open documentation quickly, but only as shortcuts to the same volume-owned screen.

## New screen

Added `VolumeDocumentationActivity`.

It receives:
- dataset/tree context;
- Classic dataset entrypoint;
- volume title;
- volume entrypoint.

It does NOT receive or require:
- section code;
- section title;
- section entrypoint.

The activity resolves `documentation_path` directly from `runtime-ir-index.json` using the current volume entrypoint.

## Volume screen entry

`ModernVolumeActivity` now has a direct `Документація` button above the section list.

This means the user can open:
- Загальна документація;
- Запобіжники;
- Довідка;
- other documentation categories present in that volume,

without entering an arbitrary section first.

## Section shortcut

The existing `Документація` button inside `NativeSectionActivity` remains.

When the section runtime has a volume-level `documentation_path`, that button opens `VolumeDocumentationActivity`.

If the package is old/incomplete and no volume path exists, the previous section-local documentation renderer remains as compatibility fallback.

## Rendering/navigation model

The new activity:
- renders the volume documentation menu natively;
- renders nested volume documentation panels/controls natively;
- preserves its panel navigation stack across Activity recreation/rotation;
- opens final HTML/PDF documents through the existing `ViewerActivity`;
- passes the same volume context to ViewerActivity so Modern/Classical navigation remains consistent.

This first version deliberately reuses the existing document viewer rather than duplicating PDF/HTML rendering logic inside the documentation Activity.

## Data ownership boundary

Documentation is scoped strictly to one Renault volume.

Never merge documentation between different volumes/configurations, even when menu labels are identical.

## Section graph cleanup

v0.5.31 does NOT yet delete section-local `GENE / PLATFUSI / AIDE` graph data.

Reason:
- first prove phone parity for the new dedicated screen;
- keep compatibility fallback during migration;
- only after phone PASS remove verified duplicate documentation actions/panels/documents from section shards.

That later cleanup is a converter/package change.

## PDF Companion preparation

This architecture is the intended canonical owner for future PDF Companion/split view:
- volume screen Documentation;
- section shortcut;
- PDF Companion pane

will all resolve the same volume documentation identity.

No Companion UI is implemented in v0.5.31.

## Packaging

APK/runtime-reader UI change only.

It consumes the volume documentation shard already produced since v0.5.29.

If Renault Menu point 9 was already run for v0.5.29 or later, point 9 is NOT required again for v0.5.31.

## Version

- versionName: `0.5.31`
- versionCode: `47`
- branch: `feat/v0.5.31-volume-documentation-screen`

## Status

Implementation complete on feature branch.
CI and phone validation pending.


## Merge / CI

PR #107 merged to main as:
`6fae1c82b37e12a10938e428383e08aff3d8135c`.

Green tested source:
`b73d0a4893318233e2b222711a4bbe82740dd5fb`.

Key runtime/version blobs were verified identical between tested feature source and merged main.

CI:
- Tests `36193148657` — PASS;
- Android Debug `36193148624` — PASS;
- artifact `Renault-Docs-v0.5.31-Debug`;
- artifact id `10888463798`;
- APK SHA-256 `c1318bf7fff75e793f0f8270aa1a4f7587deb1b7b2c12f57410160b839779cd5`;
- artifact ZIP SHA-256 `add2d7d8319eecd2a2e664f587adbc55bcb9d1a7ed4f3b22d9f158b4daaff589`.

Status: CI PASS; phone validation pending.


## Phone validation progress — 2026-09-26

After regenerating the package with Renault Menu point 9, the direct volume Documentation entry works for:
- volume: `NT8236A`;
- date: `2002-11-18`.

Observed root documentation categories:
- Загальна документація;
- Запобіжники;
- Довідка.

The earlier "volume-level documentation відсутня" screen was caused by stale package data rather than the v0.5.31 Activity.

Status: **PHONE PARTIAL PASS**.

Still to verify before closeout:
- open child items under the three categories;
- section 103/105 Documentation shortcut lands on the same dedicated volume screen;
- another volume remains isolated;
- rotation/back hierarchy inside nested documentation.


## Phone closeout — 2026-09-26

User reported PASS for the remaining v0.5.31 gates.

Accepted:
- child documentation navigation;
- section 103/105 shortcut parity;
- another-volume isolation;
- rotation/back hierarchy.

ARCH-008 dedicated volume documentation screen is closed.

Status: **PHONE PASS**.
