# Renault Docs v0.5.3 — Hybrid section shell

## Why v0.5.2 was not enough

Real-phone CMP101 verification:
- actions 1–6 all failed;
- the expected combo/select itself was absent.

That proves the direct section entrypoint is only one child document from the original Renault frameset. Missing sibling frames cannot be reconstructed reliably from click handlers alone.

## New architecture

Native section navigation stays.

But when the user taps 101 / 103 / 105 / ...:

1. Android opens the full original volume entrypoint/frameset.
2. A hybrid bridge waits for all same-origin Renault frames to load.
3. It identifies the old navigation frame by scoring how many unique three-digit Renault section codes it contains.
4. It finds the requested section code in that original navigation document.
5. It triggers the original Renault click/onchange handler.
6. After the section is selected, the old navigation frame is collapsed to zero size when possible.

Result:
- Renault keeps its real parent/sibling frame context;
- original combo boxes, selectors and internal panels can exist again;
- user still enters through the native Technical Blue 101/103/... screen;
- the old section menu is not intended to remain visible after auto-selection.

## Compatibility layers

The standalone compatibility bridge from v0.5.1/v0.5.2 remains for genuinely standalone legacy documents.

Hybrid section launches no longer depend on that emulation path.

## Safety/fallback

If auto-selection cannot find the requested code, the full original frameset remains loaded instead of navigating to a broken child fragment.

No dataset/Fast Pack refresh is required.
