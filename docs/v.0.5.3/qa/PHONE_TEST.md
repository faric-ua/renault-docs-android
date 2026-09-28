# v0.5.3 Phone Test

Install over v0.5.2. Point 9 is not required.

## Primary test

1. Modern → open the same volume.
2. Tap native section 101.
3. Expected:
   - the full Renault section environment initializes;
   - the old left 101/103/... navigation is automatically selected and then collapsed/hidden;
   - CMP101 should contain the controls that were missing in v0.5.2, including its combo/select if the original frameset provides it.

## CMP101 controls

Retest all six top actions:
- 1
- 2
- 3
- 4
- 5
- 6

Then test the combo/select.

Report:
`1 +/- · 2 +/- · 3 +/- · 4 +/- · 5 +/- · 6 +/- · combo +/-`

## Additional regression

- open section 103;
- open CMP141;
- open a PDF;
- verify Modern returns to the same native volume section list;
- verify Classic still opens the untouched original volume;
- verify PDF zoom presets / Fit width / Save PDF.

If the old left section menu remains visible but controls work, record that separately. Functionality comes first; frame collapsing can be tuned without changing the hybrid architecture.
