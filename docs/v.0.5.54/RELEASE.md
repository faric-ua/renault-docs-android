# Renault Docs v0.5.54 — Add UX + lifecycle-safe Help

Status: **IMPLEMENTED / CI PASS / PHONE TEST PENDING**

## Scope

- Home microcopy cleanup;
- empty-project copy cleanup;
- one Project `Додати` tile with `Авто` / `Вручну`;
- visible action hints: `.rdpkg · один том` and `Папка / SAF`;
- compact secondary action typography uses one shared 11sp token across Home/Project action tiles;
- reusable lifecycle-safe Help windows styled with Renault Docs dark surface/border/accent;
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


## CI

- Tests `36664711140` — PASS;
- Android PR Check `36664711278` — PASS;
- PR #13 phone acceptance is still required before merge.


## Final UI consolidation

- all app-owned AlertDialogs are audited and themed through shared `DialogUi`;
- current inventory: 9 dialogs total (1 Help + 3 Settings + 5 Project);
- Settings radio/list dialogs, Project confirmations/actions/progress, and Help share one Renault Docs dialog family;
- OS-owned SAF/DocumentsUI remains outside app theming;
- Converter/Legacy service cards use a folder pictogram instead of the word "папка" to prevent wrapping and equalize layout.


## Final visual hierarchy pass

- Home Add and Tools each use one parent tile with two child actions;
- tool folder icon is larger and right-aligned;
- project/volume/dataset/section identities use shared secondary title color;
- Modern active mode, Classic actions, search fields, and Create Project primary action use the Renault Docs visual system;
- no payload, Runtime IR, or rdpkg rebuild is required.
