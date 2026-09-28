# Renault Docs v0.5.6 — Deterministic Modern frame projection

## Purpose

v0.5.5 captured the real NT8183A frame tree on the phone.

v0.5.6 replaces generic menu-score hiding with the exact runtime contract observed on-device.

## Phone evidence driving this change

The real top runtime settles on 'RUS/HTM/ENTREE.HTM' with five named frames:

- 'titre' — Classic title/header;
- 'org' — 101/103/... navigation list;
- 'menu' — active section toolbar/menu;
- 'nav' — active section inner navigation/control frame;
- 'doc' — active document/PDF frame.

'titre + org' are the left Classic navigation branch.

'menu + nav + doc' are the working runtime that must remain alive.

## v0.5.6 behavior

Hybrid injection:
- no longer depends on the final page still being the original INDEX.HTM;
- runs for the finished legacy HTML document in section mode, so it survives the transition to ENTREE.HTM.

Section selection:
- targets named frame 'org';
- locates the requested 3-digit code using rendered text nodes as well as ordinary clickable elements;
- dispatches the original click path;
- does not declare success until the requested section is observable in named 'menu' / 'nav' runtime frames.

Projection:
- finds the common nested FRAMESET containing 'titre' and 'org';
- finds that branch inside its outer FRAMESET;
- sets the branch dimension to zero at the outer geometry boundary;
- preserves 'menu', 'nav', and 'doc';
- keeps the hidden left branch alive instead of deleting it;
- guards the projection with MutationObserver so legacy frameset rewrites do not restore the Classic column.

Diagnostics:
- DBG remains available for v0.5.6 phone validation;
- hybrid debug state now records phase, trigger attempts, and the exact projection contract.

## Important

No dataset/Fast Pack refresh is required.

The first phone gate is NT8183A → Modern → 101.

Expected result:
- no visible Classic left title/code column;
- 101 opens automatically;
- working section controls remain;
- PDF/document content remains;
- no 'Modern shell: Classic runtime не вдалося повністю сховати.' fallback.
