# Renault Docs v0.5.4 — Modern hybrid shell

## Phone result from v0.5.3

Functional result:
- legacy section runtime works;
- controls/combo work.

UX problem:
Modern section launch visibly opens the Classic Renault frameset.

## Goal

Keep the complete Classic frameset alive only as the internal runtime, while the user remains in a Modern launch flow.

## Changes

### No Classic flash during Modern launch

When a native section is opened:
- the WebView starts hidden;
- the app shows a small `Відкриваю Modern · 101…` status;
- the complete legacy frameset loads invisibly;
- original Renault section selection runs inside that runtime;
- the visible WebView is revealed only after the hybrid selector reports success.

If the selector cannot complete within the timeout, the runtime is revealed as a safe fallback rather than leaving a blank screen.

### Stronger legacy navigation collapse

After the target section is selected:
- the detected legacy 101/103/... menu frame is collapsed;
- any additional high-confidence three-digit section menu frames are also collapsed;
- frames are kept alive but made zero-size/non-interactive;
- collapse is repeated immediately and after short delays in case legacy scripts rewrite frameset geometry.

This preserves cross-frame JavaScript while removing the old section chooser from the Modern path.

## Boundary

v0.5.4 does not yet convert every inner Renault toolbar, combo or illustration selector into native Android UI.

Those controls can remain legacy content inside the Modern shell until their own migration wave.

No dataset/Fast Pack refresh is required.
