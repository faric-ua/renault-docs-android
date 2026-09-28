# v0.3.1 Phone Test

## A — Build Fast Pack

After pulling main, run Renault Menu item:

`9 — Оновити Fast/Modern package`

Expected:
- package command completes;
- output reports Fast Pack path, file count and bytes;
- dataset gets one `_renault/fast-content-*.zip`;
- old Fast Pack archives are removed.

## B — Install

Install v0.3.1 over v0.3.0.
No uninstall should be required.

## C — First activation

1. Open Laguna.
2. Watch Modern status.
3. Expected: `Fast Pack: готую…`, then `Fast Pack готовий` or `Fast Pack активний`.
4. The first copy may take some time once.

## D — Speed comparison

1. Open NT8236.
2. Navigate through at least five left-menu entries.
3. Return to previously opened entries.
4. Back to Modern, then reopen NT8236.
5. Expected: legacy pages/resources load substantially faster than SAF-only v0.3.0.

## E — Regression

- PDF render;
- PDF Save;
- Classic;
- Back;
- rotation.

Do not mark PERF-002 PASS without real-phone comparison.
