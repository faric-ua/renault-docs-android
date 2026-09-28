# v0.4.3 Phone Findings → v0.4.4

Date: 2026-09-24

## Confirmed working

- SEARCH-RESUME-001 fixed: reopening Search immediately resumes matches.
- PDF Fit width vector arrows are visually centered.
- Classic tile ordering remains chronological.

## BACKUP-UX-003 — Backup actions look detached

Observed:
The selected folder is shown inside a rounded settings card, but `Скинути вибір папки` is rendered as a full-width button outside that card.

Effect:
The action visually looks unrelated to the setting it controls.

v0.4.4:
Move folder actions inside the Backup card.

## BACKUP-UX-004 — redundant folder basename

Observed:
The card can show:
`Backup`
followed by:
`Внутрішня пам’ять/.../Backup`.

Effect:
The first line adds no useful information and looks like a second heading.

v0.4.4:
Show only a `Вибрана папка` label plus the readable path.
