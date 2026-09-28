# v0.5.8 Phone Test — Native Runtime IR preview

## Before installing APK

Because converter/package output changed:

1. Renault Menu → 5 — update project.
2. Renault Menu → 9 — regenerate Fast/Modern package / Runtime IR v2.
3. Renault Menu → 12 — export Runtime IR coverage.
4. Upload `Runtime-IR-Coverage.json` for cross-year menu audit.

## Native 101 gate

Install v0.5.8.

Open:

```text
Modern
→ NT8183A · 2001-01-22
→ 101 · ПРИКУРИВАТЕЛЬ
```

Expected:

- no Classic frameset splash on section open;
- header says Runtime IR v2 / native preview;
- menu buttons come from JSON;
- SCH/NM/PC/GENE/direct PDF actions are available;
- SCH renders its selector/options natively;
- NM renders its variant selector natively;
- PC renders its document action natively;
- direct PDF buttons open the Android PDF layer;
- CRITERE/structured HTML shows native text/table content;
- NM composite document can expose the drawing PDF and native detail table;
- Classic button still opens the old working runtime.

## Newer-year safety gate

Do not claim full native coverage for all 10 volumes from the 101 test.

The exported coverage report must be reviewed for:

- menu labels not seen in 2001;
- `legacy-javascript` actions;
- compiler warnings;
- new panel/control/document types.

Unknown actions must keep working via Classic fallback until their semantics are compiled.
