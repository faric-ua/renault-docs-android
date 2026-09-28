# v0.5.6 Phone Test — deterministic frame projection

No Renault Menu point 9 is required.

## Primary gate

1. Install v0.5.6 over v0.5.5.
2. Open Modern.
3. Open NT8183A · 2001-01-22.
4. Open section 101.
5. Do not touch the legacy section list manually.

PASS requires all of the following:

- section 101 is selected automatically;
- the left Classic column (titre + org) is not visible;
- the right runtime remains visible and usable;
- the section toolbar/control area still works;
- the same controls previously verified in v0.5.3 still respond;
- document/PDF loading still works;
- there is no Modern-shell timeout/fallback message.

## Functional regression check

Inside 101:
- use several working legacy buttons/controls;
- move through at least two document choices;
- verify the PDF/document frame changes normally;
- verify ⇩ PDF still exports the original PDF;
- tap Modern and confirm return to the same volume's native section list.

## Diagnostic capture

If the primary gate fails:

1. leave the failed state exactly as-is;
2. tap DBG;
3. copy the full report;
4. paste it into the project chat.

If the primary gate passes, one post-success DBG capture is still useful. Expected hybrid state:

- done: true;
- phase: projected;
- projection navigation frame: org;
- title frame: titre;
- preserved frames: menu, nav, doc;
- outer projection value should contain a zero-width Classic branch.

## Secondary gate

After 101 passes, repeat with 103.

103 does not need a separate build unless its real frame behavior differs.
