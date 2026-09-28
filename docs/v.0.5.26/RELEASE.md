# Renault Docs v0.5.26 — full Classic catalog parity

Date: 2026-09-25

## Goal

Close BUG-004 before continuing the in-app converter execution flow.

The old Modern section discovery only accepted exactly three decimal digits. Classic navigation can contain a much broader Renault identifier space, including examples observed on phone/source:
- 101;
- 1405 / 1406 / 1407 / 1412;
- R15 / R21 / R24 / R262 / R265 / R325;
- MA / MAH / MYH / MB / ME / MG / MH / ML / MQ / MT / MW;
- NA / NC / NH / NT / NU.

## Catalog contract

v0.5.26 treats the Classic section identifier as an opaque string.

Discovery now:
- accepts 3–4 digit numeric IDs;
- accepts short alphabetic Renault IDs;
- accepts short alpha+numeric IDs;
- resolves every candidate link to a real local HTML target before keeping it;
- preserves Classic source order;
- deduplicates only the same display code + same resolved entrypoint;
- keeps two entries with the same display code when they resolve to different targets.

Modern no longer re-sorts the section list numerically on Android.

## Runtime IR duplicate-code contract

A display code alone is not a unique identity.

The sharded Runtime IR index now contains an ordered `section_entries` array with:
- `code`;
- `entrypoint`;
- `path`.

The old `sections: { code -> path }` map is retained only as a backward-compatible first-occurrence lookup.

v0.5.26 Android resolves the selected section using:
- volume entrypoint;
- section display code;
- section legacy entrypoint.

Therefore duplicate display codes can point to distinct Runtime IR shards without collision.

## Schema

`modern-sections.json` schema version: 2.

Runtime tree schema remains v2. The shard index gains an additive `section_entries` field.

## Packaging

This is a converter/package data-contract change.

After installing/updating the app/repository:
- Renault Menu → 5;
- Renault Menu → 8;
- install APK;
- Renault Menu → 9 is REQUIRED before phone parity testing.

## Version

- versionName: `0.5.26`
- versionCode: `42`
- branch: `fix/v0.5.26-classic-catalog-parity`


## Merge / CI

Merged source:
`03b0eb4a27fa0c0f076fb1cca9524a6109130272`

Main CI:
- Tests `36171369633` — PASS;
- Android Debug APK `36171369588` — PASS;
- artifact `Renault-Docs-v0.5.26-Debug`;
- artifact id `10880925610`;
- APK SHA-256 `3f42d50fc3cf335545380d6c7a47b114cbe92abdbba7d8d158028795f7a57413`;
- artifact ZIP SHA-256 `2e4c62f2d10ba671ba896d31dd9a766d6788ea03b238894371ec937065c570df`.

Status: CI PASS; phone Classic↔Modern parity validation pending.
