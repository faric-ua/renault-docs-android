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

Pending CI and phone execution.
