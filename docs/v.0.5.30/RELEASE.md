# Renault Docs v0.5.30 — high-zoom PDF scrolling fix

Date: 2026-09-25

## Problem

At 250–400% zoom, scrolling could reveal a blank placeholder page that did not render until zoom was reduced.

Real-phone evidence showed a stale counter such as `13 / 13` while the viewport was already over page 11.

Root cause:
- current page tracking relied on IntersectionObserver thresholds `[0.25, 0.5, 0.75]`;
- at 400%, a PDF page can be much taller than the viewport, so less than 25% of the page can ever be visible;
- the observer therefore may never promote the newly visible page to `currentPage`;
- v0.5.28 high-zoom memory policy retained radius 0, so scroll-time `loadAround(staleCurrentPage)` evicted the page the user was actually looking at.

## Fix

High-zoom current-page tracking no longer depends only on intersection percentage.

During nested PDF scroll:
- compute the vertical center of `#pdfViewport`;
- select the page containing that center;
- if no page contains it exactly, select the page whose center is nearest;
- update the counter/current-page state before render scheduling.

Memory/render policy at >=250%:
- current page uses fast tier while scrolling and full-quality render after idle;
- retain radius is 1 instead of 0;
- preload radius is 1 instead of 0;
- neighbors use a lightweight render capped at 1200 px;
- full-quality current page remains capped at 3000 px;
- scrolling tier remains capped at 1600 px.

This means the user should always see the next/previous page while scrolling at 300–400%, but only the current page is upgraded to the expensive high-quality render.

## Documentation architecture note

v0.5.29 per-volume documentation shard remains unchanged in this release.

Recommended next refactor:
- create a dedicated `VolumeDocumentationActivity`;
- expose `Документація` directly from the volume screen;
- keep the section `Документація` button only as a shortcut to the same volume-owned screen;
- after phone parity, remove duplicated documentation actions/documents from section shards;
- never share documentation across different volumes/configurations.

This is intentionally separate from the PDF bug fix.

## Packaging

APK/runtime UI only.
No converter/package data changes from v0.5.29.
If point 9 was already run for v0.5.29, it is NOT required again for v0.5.30.

## Version

- versionName: `0.5.30`
- versionCode: `46`
- branch: `fix/v0.5.30-pdf-high-zoom-scroll`

## Status

Implementation complete on feature branch.
CI and phone validation pending.


## Merge / CI

PR #106 merged to main as:
`2a342aa505378b651baaeef9fe44b555e6836896`.

Green tested source:
`89160047ff9a4208b547e00bd0982b22b91a45df`.

Runtime/version/test blobs were verified identical between tested source and merged main.

CI:
- Tests `36186156147` — PASS;
- Android Debug `36186156169` — PASS;
- artifact `Renault-Docs-v0.5.30-Debug`;
- artifact id `10885104783`;
- APK SHA-256 `06a3a032361ba18f6f925949b1eb959d9fa69320aba51d7c974cd177426f62ab`;
- artifact ZIP SHA-256 `9db989fab0602ffd52ae8a7adc291cc1bf86f8ce814dafc555f0330f15dd0267`.

Status: CI PASS; phone validation pending.


## Phone closeout — 2026-09-26

User confirmed that high-zoom PDF page loading now behaves correctly at 400%.

Accepted phone result:
- pages continue loading while scrolling at 400%;
- reducing zoom below 200% is no longer required to make pages appear;
- v0.5.30 high-zoom tracking/preload fix is accepted.

Status: **PHONE PASS**.
