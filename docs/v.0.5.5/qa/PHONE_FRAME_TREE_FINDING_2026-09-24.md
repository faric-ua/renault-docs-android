# v0.5.5 phone frame-tree finding — NT8183A / section 101

Date: 2026-09-24

Status:
- v0.5.5 diagnostic goal: PASS;
- enough real-device evidence was captured to stop using generic frame-score hiding;
- v0.5.4/v0.5.5 Modern presentation failure now has a concrete cause and a deterministic projection target.

## Captured states

Two reports were captured from:

- volume: NT8183A · 2001-01-22;
- requested Modern section: 101;
- top runtime URL: 'RUS/HTM/ENTREE.HTM'.

State A:
- initial Classic/Visu Schema runtime before manual section selection.

State B:
- after manually selecting section 101 and using the working legacy controls.

## Real frame tree

The top document has exactly five reachable frame windows and no deeper browsing-context nesting in this capture:

| path | name | initial URL | working 101 URL | role |
|---|---|---|---|---|
| root/0 | titre | RUS/HTM/CTITRE.HTM | same | Classic left title/header |
| root/1 | org | RUS/HTM/CODE.HTM | same | Classic 101/103/... section-code navigation |
| root/2 | menu | RUS/HTM/MENU.HTM | RUS/HTM/MENU/101.HTM | section toolbar/menu |
| root/3 | nav | COMMUN/HTM/BLANK.HTM | COMMUN/HTM/PC/101.HTM | section-specific inner navigation/control surface |
| root/4 | doc | RUS/DOCUMENT/CLAUSLEG.PDF | COMMUN/PDF/PC/S7.pdf | working document/PDF content |

Phone geometry at capture time:

- 'titre': x=0, width=218;
- 'org': x=0, width=218;
- 'menu': x=224, width=756;
- 'nav': x=224, width=756;
- 'doc': x=224, width=756.

This proves that 'titre + org' form the visible Classic navigation column on the left, while 'menu + nav + doc' are the working right-side runtime that must remain alive.

## Frameset nesting inferred directly from the reported parent geometry

'titre' and 'org' share the same immediate frameset:

- rows: '82,510';
- cols: '*'.

'menu' and 'nav' share an immediate frameset:

- rows: '50%,50%'.

'doc' belongs to the right-side frameset:

- rows: '150,543';
- cols: '*'.

Therefore the previous v0.5.4 approach was collapsing only an inner child ('org') instead of collapsing the entire left nested frameset at the outer column boundary.

The Modern projection target for this Renault generation is:

- keep the full frameset runtime loaded;
- keep 'titre' and 'org' alive but project their common left frameset branch to zero width;
- preserve 'menu', 'nav', and 'doc' exactly as live sibling runtime content.

## Section-selection evidence

Before manual section selection:

- 'menu' = 'RUS/HTM/MENU.HTM';
- 'nav' = 'COMMUN/HTM/BLANK.HTM';
- 'doc' = 'RUS/DOCUMENT/CLAUSLEG.PDF'.

After section 101 becomes active:

- 'menu' = 'RUS/HTM/MENU/101.HTM';
- 'menu' title = 'CMP 101';
- 'nav' = 'COMMUN/HTM/PC/101.HTM';
- 'nav' title = 'CMP 101';
- 'doc' = 'COMMUN/PDF/PC/S7.pdf'.

These are deterministic readiness signals. A Modern launch must not report ready immediately after firing a synthetic click; it must wait until the named runtime frames show the requested section.

## Why v0.5.4 timed out

Both phone reports show:

'hybridStates: []'

The existing injection gate only called the hybrid shell when the finished URL exactly matched the configured root entrypoint:

'Laguna X74 NT8183A 2001_01_22/INDEX.HTM'

But the real top-level runtime settles on:

'Laguna X74 NT8183A 2001_01_22/RUS/HTM/ENTREE.HTM'

Any JavaScript state created on the transient root page is lost when the top document changes. The final ENTREE.HTM page therefore had no active hybrid state, which explains the Android warm-up timeout/fallback.

## Diagnostic heuristic finding

The report's old heuristic returned:

- 'menuCandidates: []';
- 'comboCandidates: []'.

However 'org' visibly contains the complete 101/103/... text list. This confirms that generic DOM scoring is the wrong contract for this runtime. The real frame names and frame transitions are stronger evidence.

Also, 'selectCount' is zero in both captured states, despite working legacy selection/control behavior. Do not require a literal HTML <select> as a Modern readiness condition.

## v0.5.6 decision

Use the real frame contract:

1. inject the hybrid logic into the final legacy HTML top document, including ENTREE.HTM;
2. select the requested code through named frame 'org';
3. accept the section only after 'menu' / 'nav' expose the requested code;
4. find the common parent frameset of 'titre' and 'org';
5. collapse that whole branch in its outer frameset (cols/rows) to 0;
6. keep 'menu', 'nav', and 'doc' loaded and visible;
7. lock the outer projection with a small MutationObserver in case legacy code rewrites frameset geometry;
8. retain DBG through phone validation.

This is the basis of v0.5.6.
