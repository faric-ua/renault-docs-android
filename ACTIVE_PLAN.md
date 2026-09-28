# Renault Docs — ACTIVE PLAN

Updated: 2026-09-28

Purpose: live crash-recovery checklist. Read after `CURRENT_HANDOFF.md`; continue from the first unchecked item.

## Current repository baseline

- [x] v0.5.50 Android-native `.rdpkg` export implemented.
- [x] v0.5.50 native `.rdpkg` round-trip recorded as real-phone PASS on main.
- [x] Mandatory active-plan rule added to the assistant/project workflow.
- [ ] Reconcile the root `CURRENT_HANDOFF.md` with the latest v0.5.50 main baseline before starting another feature.
- [ ] Define the next release goal with the user and create its release documentation skeleton before feature code.
- [ ] Replace the remaining generic items here with the exact ordered checklist for that release.
- [ ] Execute and mark each verified implementation/CI/phone step as it completes.

## Rule

After every successful project-progress step:
1. mark only the evidence-backed checkbox `[x]`;
2. update relevant QA/findings;
3. update `CURRENT_HANDOFF.md` if the resume point changed;
4. keep the first unchecked item as the next action.

Do not rely on chat memory as the only progress record.
