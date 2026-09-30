# Renault Docs v0.5.54 — Add UX + lifecycle-safe Help

Status: **IMPLEMENTED / CI PENDING**

## Scope

- Home microcopy cleanup;
- empty-project copy cleanup;
- one Project `Додати` tile with `Авто` / `Вручну`;
- reusable lifecycle-safe Help windows;
- Help on the complex library/project/converter/Modern/native/documentation surfaces;
- CreateProject text survives rotation;
- Project modal state survives rotation without automatic actions;
- UI/lifecycle audit documented in `UX_AUDIT.md`.

## Non-goals

- no `.rdpkg` payload/schema change;
- no Runtime IR regeneration;
- no Laguna package rebuild;
- no destructive storage changes.

## Known follow-up

The audit found the direct `.rdpkg` install worker is still Activity-owned. It should become service/run-store backed in a separate lifecycle hardening change.
