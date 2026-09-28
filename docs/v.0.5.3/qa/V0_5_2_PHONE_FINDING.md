# v0.5.2 Phone Result → v0.5.3 Architecture Change

Date: 2026-09-24

## CMP101 result on v0.5.2

Retest result:
- 1: FAIL
- 2: FAIL
- 3: FAIL
- 4: FAIL
- 5: FAIL
- 6: FAIL
- combo/select: not present at all

This invalidates the direct-child standalone approach for this class of Renault sections.

## Architectural conclusion

The section entrypoint extracted from the old menu is not a self-contained section screen. It is a child document that originally lived together with sibling frames inside the volume frameset.

Because the combo itself is absent, additional onclick/parent.frames emulation inside the child fragment cannot restore the missing UI reliably.

## v0.5.3 direction

Keep native Technical Blue 101/103/... navigation, but preserve the complete original Renault runtime underneath:

1. load the full original volume frameset;
2. identify the old navigation frame after all sibling frames exist;
3. find the requested three-digit section in that real menu;
4. invoke the original Renault handler;
5. collapse the old navigation frame after selection while leaving it alive.

This preserves:
- parent/top frame relationships;
- sibling frames;
- combo/select controls;
- Renault's original cross-frame JavaScript;
- all secondary panels.

Native remains the user-facing section chooser.
