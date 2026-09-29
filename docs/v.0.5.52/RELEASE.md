# Renault Docs v0.5.52 — terminal status polish

Status: **READY / CLOSED — 2026-09-30**

## Scope

- terminal COMPLETE / CANCELLED / FAILED status can be dismissed with a right-side `×`;
- active PREPARING / IMPORTING never exposes that terminal dismiss control;
- dismissed terminal presentation stays dismissed after Activity recreation/reopen;
- dismissal does not delete project, source, package or run metadata;
- RDPKG progress uses correct Ukrainian file-count forms;
- the already-open native preparation dialog now shows live progress instead of a frozen snapshot.

## Accepted source and CI

Final phone-accepted source:
`8a364ef9e50d0bd81273e9d419321284abdfed03`

- Tests run `36639849368` — PASS;
- Android PR Check `36639849222` — PASS;
- trusted candidate Android Debug run `36640408798` — PASS;
- candidate APK SHA-256 `2a2fc9c952ac24c11b3022786b1f6e1eccd98c5a595532317f15f080d01797aa`;
- signer SHA-1 `4102350e2787fd538bbf58a219293a132235e618`.

## Phone acceptance

Real-phone checks completed on 2026-09-30:

- in-place install preserved existing projects and data;
- app opened as v0.5.52;
- Megane II remained at 2 volumes; Laguna II and Kangoo II remained present;
- terminal CANCELLED status showed the new `×`;
- tapping `×` hid the terminal status;
- dismissal remained hidden after repeated rotation and after leaving/reopening Megane II;
- active PREPARING showed progress and did not show terminal `×`;
- cancellation returned to terminal CANCELLED presentation;
- live progress in the open “Підготовка .rdpkg виконується” dialog updated while the operation continued.

The `1 файл` wording is covered by JVM/contract tests; no extra destructive phone scenario was run only to force that exact counter value.

## Distribution closeout

- PR #6 squash-merged to public `main` as `31c26e9c24abab6fb491f8aa9290c5395112a2ca`;
- public-main Tests `36642605889` — PASS;
- public-main Android Debug `36642605895` — PASS;
- artifact id `11067202339`;
- stable signer SHA-1 `4102350e2787fd538bbf58a219293a132235e618`;
- public-main APK SHA-256 `2a2fc9c952ac24c11b3022786b1f6e1eccd98c5a595532317f15f080d01797aa`.

The public-main APK hash exactly matches the already installed and phone-accepted final candidate. Therefore no separate reinstall is required: it would install the identical signed binary and add no new evidence.

Final verdict:
**v0.5.52 READY / CLOSED.**

Next work is the read-only audit of historical `*_android` folders before any cleanup.
