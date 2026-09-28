# v0.5.9 Phone test — Runtime IR shards

## Required preparation

1. Renault Menu → 5 — update project.
2. Renault Menu → 9 — regenerate Fast/Modern package.
3. Renault Menu → 8 — install v0.5.9 APK.

Point 9 is mandatory. v0.5.8 datasets do not contain the new `runtime-ir-index.json` and per-section shards.

## Gate

Open:

```text
Modern
→ NT8183A · 2001-01-22
→ 101 · ПРИКУРИВАТЕЛЬ
```

Expected:

- no long stall on `Читаю Runtime IR v2…`;
- no 268 MB allocation / OOM;
- native Runtime IR preview appears;
- menu actions render from JSON;
- SCH/NM/PC/GENE/direct PDF actions can be tested;
- Classic fallback remains available.

## If it fails

Capture:
- screenshot of the visible error;
- whether it failed before or after the native menu appeared;
- approximate wait time from tapping 101 until failure/success.

Do not use the old v0.5.8 package for this test.
