# v0.4.4 Phone Test

Install over v0.4.3.

## Backup card
1. Open Settings → Backup.
2. Confirm folder information and actions are all inside one card.
3. Confirm there is no detached reset button below the card.
4. Confirm the card contains:
   - Папка резервних копій
   - Вибрана папка
   - readable path
   - Змінити папку
   - Скинути вибір
5. Confirm the standalone folder name `Backup` is not duplicated above the path.
6. Tap Змінити папку and select another folder.
7. Confirm the path updates.
8. Tap Скинути вибір.
9. Confirm the card changes to `Не вибрано` and only `Вибрати папку` remains.
10. Confirm no files are deleted.

## Regression
- Search reopen works immediately with unchanged saved query.
- Fit width icon stays centered.
- Classic volume order stays chronological.
