# v0.5.5 Phone Test — Frame tree capture

No point 9 is required.

## Primary capture

1. Install v0.5.5.
2. Modern → the same volume → section 101.
3. Wait until the visible Classic/Visu Schema fallback appears.
4. Tap `DBG`.
5. Tap `Копіювати`.
6. Paste the complete report into the ChatGPT project conversation.

Do not manually clean or shorten the report.

## If practical, capture two states

A. Immediately after the Classic/Visu Schema fallback is visible.

B. After navigating inside the Classic runtime until the working CMP101 controls/combo are visible, then tap `DBG` again.

Two reports are ideal because they let us compare:
- initial splash/runtime hierarchy;
- working section hierarchy.

## Secondary capture

If section 103 reaches a visibly different layout, capture a third report there.

## What we need to identify

From the report we will determine:
- exact old 101/103 navigation frame;
- exact combo frame;
- exact working content frame;
- splash frame;
- real nested frameset geometry before/after section selection.

Then v0.5.6 can hide/project exact frames instead of using generic menu-score heuristics.
