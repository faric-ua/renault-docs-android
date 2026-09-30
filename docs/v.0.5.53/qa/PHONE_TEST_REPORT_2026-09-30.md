# v0.5.53 phone test report — 2026-09-30

Result: **PARTIAL — first candidate needs group-order refinement**

Target:
`Laguna II → NT8183A · 2001-01-22`

Checks:
- [ ] in-place install preserves projects/data;
- [ ] volume still opens as native;
- [x] numeric section list uses natural code order;
- [ ] `R...` connector IDs appear after all numeric-leading IDs and before other alphabetic IDs;
- [ ] `101 / 101_1 / 101_2`-style groups remain adjacent where present;
- [ ] numeric values sort numerically, not lexicographically;
- [ ] opaque IDs such as `R70` remain present;
- [ ] duplicate display codes still route to their own entrypoints;
- [ ] search preserves the same sorted presentation;
- [ ] representative section open works.
