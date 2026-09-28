# v0.5.0 Phone Test

## Preparation

1. Pull current main.
2. Run:
   `Renault → 9 — Оновити Fast/Modern package`
3. Confirm package output includes:
   `Sections: .../_renault/modern-sections.json`
4. Install v0.5.0 APK.

## A. Modern volume → native sections

1. Open Renault Docs in Modern.
2. Open NT8236A (or another technical-documentation volume).
3. Expected:
   - old left Renault frame menu does NOT appear as the first screen;
   - a native Technical Blue one-column section list appears;
   - rows show codes such as 101 / 103 / 105 / 107 / ... if discovered;
   - status shows `Розділів: N · native`.

Take a screenshot of this screen.

## B. Section search

1. Tap Search in the native volume header.
2. Search for:
   - `101`;
   - `генератор` or another visible label.
3. Expected:
   native rows filter immediately.

## C. Open section

1. Tap a native row such as 103.
2. Expected:
   existing viewer opens the corresponding legacy content.
3. Confirm PDF/HTML still works as before.

Important:
if a direct section entrypoint depends on missing frame context and renders incorrectly, record the exact code/title. That is a parser/navigation mapping finding, not a reason to remove the native list.

## D. Modern return

1. While inside section content, tap Modern.
2. Expected:
   return to the same volume's native section list, not the 10-volume root.

## E. Classic fallback

1. From native section screen tap Classic.
2. Expected:
   original volume frameset opens.
3. Tap Modern from Classic.
4. Expected:
   return to this volume's native section list.

## F. Fallback safety

If a volume has no discovered sections:
- app shows a native fallback explanation;
- `Відкрити Classic` still opens the volume;
- app does not crash.

## Regression

Verify:
- volume sorting remains chronological;
- page Search still works;
- PDF zoom preset dropdown works;
- Fit width works;
- original PDF Save works;
- Settings/Backup remains intact.
