# v0.5.62 Phone Test

## Gate A — catalog discovery

1. Open Home → Готові проєкти.
2. App opens `Каталог Renault Docs` inside Renault Docs, not raw Google Drive.
3. Megane II is shown with E84/L84/K84 and two volumes.
4. Existing local volumes show `✓ Встановлено`.

## Gate B — first round-trip import

1. Remove exactly one Megane II volume from the project (do not uninstall the app).
2. Reopen Catalog.
3. The removed volume becomes selectable.
4. Select it and tap `Імпортувати вибране`.
5. Download and import progress must stay inside Renault Docs.
6. After completion, Megane II must return to 2 volumes with the original volume identity.

## Gate C — lifecycle

- rotate/reopen Catalog during download/import;
- operation must reattach instead of starting a second import.

Result: **PASS — 2026-10-06**.


## Accepted evidence

Exact accepted runtime head:
`e215ed9a29ccf7cd7e36d083e2579e24bd2a9c3f`

CI:
- Tests #451 — PASS;
- Android PR Check #369 — PASS.

Gate A — PASS.
Gate B — PASS.
Gate C — PASS.

Additional phone findings closed in the same candidate:
- literal `null` optional region text removed;
- selected checkbox survives rotation;
- completed import card survives rotation;
- catalog project/volume cards restore promptly after rotation without a second network wait.

Final state:
- Megane II = 2 volumes;
- NT8340A opens successfully;
- NT8342A remains installed.
