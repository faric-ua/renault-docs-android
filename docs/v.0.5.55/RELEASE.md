# Renault Docs v0.5.55

Status: PHONE PASS / RISK-LIFE-001 CLOSED

## Scope

Direct .rdpkg installation no longer belongs to ProjectActivity. The install is owned by a foreground service backed by a persistent run store; ProjectActivity reattaches to progress and consumes the terminal result only after project handoff.

## Accepted candidate

- version: v0.5.55
- build: 71
- candidate: 7b79195b2ee8c9457f6d07e38fc285f65989e394
- stable-signed Android Debug APK run: 37118145338
- artifact: Renault-Docs-v0.5.55-Debug

## Phone acceptance — 2026-10-03

PASS:
- installed over the existing app;
- direct .rdpkg import started in Kangoo II;
- Activity was recreated by rotation while the package was being imported;
- import continued and reached the project-mismatch handoff;
- user confirmed “Додати сюди”;
- NT8340A · 2006-04-18 was added to Kangoo II exactly once;
- project volume count became 1.

RISK-LIFE-001 is closed.

## Follow-up UX

A separate follow-up will unify long-running project/volume operation status into one lifecycle-safe progress surface. Current status/progress presentation is fragmented across inline status text, Home progress bar, native preparation dialog, and service notifications.
