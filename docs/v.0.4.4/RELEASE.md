# Renault Docs v0.4.4 — Unified Backup settings card

## Purpose

Phone-follow-up for v0.4.3.

The Backup section worked functionally, but its controls looked disconnected:
- folder information lived inside a card;
- the reset button lived outside the card;
- the folder name could duplicate the meaning of the Backup section.

## Implemented

Backup settings now form one coherent card.

Inside the card:
- title: `Папка резервних копій`;
- short description and recommended location;
- label: `Вибрана папка`;
- readable selected path only;
- action buttons inside the same card.

When no folder is selected:
- `Вибрати папку`.

When a folder is selected:
- `Змінити папку`;
- `Скинути вибір`.

The separate external reset button is removed.

The folder basename is no longer shown as a separate value above the full path, avoiding duplicated text such as:
`Backup` + `.../Backup`.

Warnings for backup paths inside an `_android/` dataset also remain inside the Backup card.

## Confirmed from v0.4.3 phone QA

- Search reopening with a preserved query now works without editing the query.
- Fit-width vector arrows are visually centered.
- Classic volume ordering remains chronological.

## Unchanged

- no Backup archive creation yet;
- no files are deleted;
- no Fast Pack or dataset rebuild is required.
