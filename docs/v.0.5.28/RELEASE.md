# Renault Docs v0.5.28 — 400% PDF zoom + fullscreen

Date: 2026-09-25

## Goal

Extend the accepted v0.5.27 pinch behavior for detailed Renault diagrams and add an explicit fullscreen document mode.

## PDF zoom

User-visible zoom range is now 50–400%.

Quick zoom menu adds:
- 300%;
- 400%.

The same shared zoom state is used by:
- pinch;
- +/-;
- typed percentage;
- quick presets.

## High-zoom memory policy

400% is display magnification, not an instruction to keep multiple giant decoded pages in memory.

To keep the current PdfRenderer/WebView architecture safe:
- full-quality render cap increases from 2400 px to 3000 px;
- active scrolling still uses the temporary <=1600 px render tier;
- at >=250% neighbor prefetch radius is 0;
- at >=250% decoded retain radius is 0;
- therefore only the current page is kept decoded at very high zoom;
- shared compressed cache remains available for reloads.

This deliberately trades a little neighbor prefetch speed for memory stability at 250–400%.

## Fullscreen PDF mode

The PDF toolbar now includes a fullscreen toggle.

Entering fullscreen:
- hides ViewerActivity global app toolbar;
- hides page-search row if open;
- hides Android status/navigation bars;
- keeps the PDF toolbar visible;
- keeps document zoom/scroll state intact.

Exiting fullscreen:
- restores app toolbar;
- restores the search row if it was open before fullscreen;
- restores Android system bars.

Android Back exits fullscreen first before navigating away.

Fullscreen state survives Activity recreation/rotation.

## Packaging

APK-only change.
Converter/Runtime IR/package data do not change.
Renault Menu point 9 is NOT required.

## Documentation optimization finding

The Modern `Документація` group currently comes from section-local Runtime IR menu items (`GENE / PLATFUSI / AIDE`).

For a volume where these routes are identical across sections, the durable optimization is to hoist them to a volume-level documentation catalog/shard and let every section reference that shared volume object.

Expected benefits:
- smaller section shards;
- no repeated documentation-route parsing per section;
- one stable volume documentation button/catalog;
- one warm/cache target per volume;
- cleaner semantics: documentation that belongs to the volume is modeled as volume data.

The physical web/PDF files are already stored once in the dataset/Fast Pack, so this is mainly a Runtime IR/catalog optimization rather than file deduplication.

This is intentionally NOT implemented in v0.5.28 because it is a converter/package contract change and would require Renault Menu point 9.

## Version

- versionName: `0.5.28`
- versionCode: `44`
- branch: `feat/v0.5.28-pdf-400-fullscreen`

## Status

Implementation complete on feature branch.
CI and phone validation pending.


## Merge / CI

PR #104 merged to main as:
`1104db917f5964db7efb41a9282eb88ccde733ee`.

Green tested feature source:
`330c4d7168712236453d76fe8fef92e17710b35c`.

Runtime source equivalence after squash merge was verified for:
- `AndroidPdfLayer.kt`;
- `SafDatasetWebViewClient.kt`;
- `ViewerActivity.kt`;
- `android/app/build.gradle.kts`.

CI:
- Tests `36179103365` — PASS;
- Android Debug APK `36179103296` — PASS;
- artifact `Renault-Docs-v0.5.28-Debug`;
- artifact id `10884005657`;
- APK SHA-256 `615fe8bb939b41a92ac999472bad0015c9a5e2b7dfaf204cb344bba778479fd2`;
- artifact ZIP SHA-256 `9016bd4ef4b3dc03d28a6b50e9037f4eb89731e875db186b69c1ee8c052b780d`.

Status: CI PASS; phone validation pending.


## Phone closeout — 2026-09-25

User confirmed the 400% zoom/fullscreen build works better and accepted it for continuation.

Phone result:
- 400% zoom behavior accepted;
- fullscreen entry/exit works;
- one UX follow-up remains: the fullscreen toolbar control did not visually indicate its active state.

That follow-up is implemented in v0.5.29 with a synchronized pressed/accent state.

Status: **PHONE PASS WITH UX FOLLOW-UP**.
