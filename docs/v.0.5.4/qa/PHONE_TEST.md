# v0.5.4 Phone Test

Install over v0.5.3. Point 9 is not required.

## Main check

1. Modern → volume → 101.
2. Expected:
   - no visible flash of the full Classic frameset while loading;
   - a brief `Відкриваю Modern · 101…` status is acceptable;
   - after load, the old 101/103/... navigation menu should not remain visible;
   - working CMP101 controls/combo from v0.5.3 must still work.

## Repeat

Open:
- 103;
- 141;
- a PDF path.

Expected:
- no regression in controls;
- PDF viewer still uses our toolbar;
- Modern button returns to the native section list.

## Report

Please report separately:
- `runtime controls: + / -`
- `old 101/103 menu visible: yes / no`
- `Classic flash during open: yes / no`

If runtime controls stay working and only some old top toolbar remains visible, treat that as the next UI migration layer, not a runtime failure.
