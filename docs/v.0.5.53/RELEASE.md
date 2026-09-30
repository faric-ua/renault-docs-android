# Renault Docs v0.5.53 — Modern section display order

Status: **MERGED / PHONE PASS — 2026-09-30**

## Scope

- Modern volume section tiles are displayed in stable natural order by Renault section code;
- converter/runtime data still preserves Classic source order;
- opaque identifiers remain opaque and are never coerced to a single numeric-ID model;
- numeric codes sort naturally, e.g. `321 < 338 < 853 < 1013`;
- suffix variants remain grouped, e.g. `101 < 101_1 < 101_2`;
- display groups are numeric-leading IDs → `R...` connectors → other alphabetic IDs;
- alphanumeric/alphabetic identifiers remain supported;
- duplicate display codes remain separate and preserve their relative source order;
- search results use the same display ordering.

## Non-goals

- no `.rdpkg` regeneration;
- no Runtime IR schema change;
- no section deduplication;
- no Classic source rewrite.

## Phone gate

Use an already imported Laguna II package, preferably `NT8183A · 2001-01-22`.

Verify:
1. section list is naturally ordered by code;
2. mixed IDs such as `R70` remain visible;
3. search still finds numeric and alphanumeric codes;
4. representative sections still route to the correct entrypoint;
5. existing projects/data survive the in-place update.

## Acceptance

- final refined candidate installed on phone;
- Laguna II NT8183A remained available in native mode;
- user confirmed final section ordering is correct;
- accepted grouping: numeric-leading IDs → `R...` connectors → other alphabetic IDs;
- Tests `36660615811` — PASS;
- Android PR Check `36660615926` — PASS;
- `.rdpkg` regeneration not required.

## Merge

- PR #12 merged as `68323e66c67f07df0e770d99ba7f7b2aa3d95b0f`;
- final PR-head Tests `36661693096` — PASS;
- final PR-head Android PR Check `36661693082` — PASS.
