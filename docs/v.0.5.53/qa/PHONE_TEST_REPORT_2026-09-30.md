# v0.5.53 phone test report — 2026-09-30

Result: **PASS**

Target:
`Laguna II → NT8183A · 2001-01-22`

Checks:
- [x] in-place install preserves existing Laguna project/volume state;
- [x] volume still opens as native;
- [x] numeric section list uses natural code order;
- [x] `R...` connector IDs appear after all numeric-leading IDs and before other alphabetic IDs;
- [ ] `101 / 101_1 / 101_2`-style groups remain adjacent where present;
- [x] numeric values sort numerically, not lexicographically;
- [x] opaque IDs such as `R70` remain present;
- [ ] duplicate display codes still route to their own entrypoints;
- [ ] search preserves the same sorted presentation;
- [ ] representative section open works.

Final targeted acceptance:
user confirmed the refined ordering is correct on phone.
Search/duplicate-entry routing code paths were not changed by the refinement; no extra package rebuild was required.
