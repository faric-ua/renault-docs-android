# Renault Docs v0.5.29 — volume documentation IR + fullscreen pressed state

Date: 2026-09-25

## Goal

1. Make the PDF fullscreen button visibly show whether fullscreen is active.
2. Model Renault documentation at the correct scope: one documentation set per volume/configuration, shared by sections inside that volume only.

## Fullscreen active state

The PDF fullscreen button now has an explicit pressed state:
- inactive: normal toolbar button;
- active: accent background/border with pressed icon treatment;
- `aria-pressed` switches between false/true;
- label/title changes to "Вийти з повноекранного режиму" while active;
- ViewerActivity is authoritative and synchronizes the HTML control after toggle, focus and Activity recreation.

## Volume documentation contract

Documentation is NOT global across the Renault dataset.

Correct ownership:

```text
Volume/configuration A
  └─ Documentation A
      ├─ GENE
      ├─ PLATFUSI
      └─ AIDE

Volume/configuration B
  └─ Documentation B
      ├─ its own GENE
      ├─ its own PLATFUSI
      └─ its own AIDE
```

Sections inside one volume may reuse the same volume documentation set.

The converter:
- inspects section menu actions `GENE / PLATFUSI / AIDE`;
- compares their resolved route signatures only inside the current volume;
- hoists them only when all non-empty signatures in that volume agree;
- refuses to hoist when targets conflict;
- creates a self-contained documentation bundle with remapped `vdoc-*` IDs so it cannot collide with section-local action IDs.

## Runtime packaging

Each qualifying volume gets one file under:

`_renault/runtime-ir/documentation/<volume-key>.json`

The runtime index stores that volume's `documentation_path`.

Section shards remain backward-compatible in v0.5.29. This first migration changes the runtime owner/read path without destructively removing the old section-local source graph. Once phone parity is proven, later cleanup can remove verified duplicate section-local documentation metadata.

The physical HTML/PDF source files remain in their original volume folders and are never merged between volumes.

## Android runtime

NativeSectionActivity:
- sees only the small `documentation_path` while loading a section;
- does NOT read the documentation shard during ordinary section startup;
- loads it lazily on the first press of `Документація`;
- caches the parsed documentation object in the current Activity;
- uses section-local documentation only as compatibility fallback if the volume shard is absent or cannot be read.

This keeps section startup independent from documentation payload size.

## Packaging

This release changes converter / Runtime IR / package output.

Renault Menu point 9 IS REQUIRED after installing v0.5.29.

## Version

- versionName: `0.5.29`
- versionCode: `45`
- branch: `feat/v0.5.29-volume-documentation`

## Status

Implementation complete on feature branch.
CI and phone validation pending.


## Merge / CI

PR #105 merged to main as:
`05c6833070ebbc5c2db8a3015bcdf1b030cfbcc6`.

Green tested source:
`db1078e4a45b5e2e2d5fecaf9d64b105acbded15`.

Runtime/compiler source blobs were verified identical between tested feature source and merged main.

CI:
- Tests `36181651229` — PASS;
- Android Debug `36181651350` — PASS;
- artifact `Renault-Docs-v0.5.29-Debug`;
- artifact id `10884272976`;
- APK SHA-256 `761404a0380ed07c47d02508a4298a393b87e9040b3390648b7c90a4c832b1e3`;
- artifact ZIP SHA-256 `bb8a0ca782aa01de9c5231e405272de7e5aff415cf5327386af92ab2873b0047`.

Status: CI PASS; phone validation pending.
