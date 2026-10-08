# Renault Docs — dialog consistency audit, 2026-10-08

## Scope and result

Audited application-owned Android `AlertDialog` builders across project operations, Home project actions, Settings, Help, and Viewer. Do **not** conflate these with Android's external SAF file picker, system permission sheets, notifications, or full-screen Activities.

**Inventory: 20 Android AlertDialogs in 5 implementation files.** Before this change 18 were passed through `DialogUi.apply()`; the Viewer section navigator and frame-debug dialog were not. After this change **20/20** use the existing shared style and explicit semantic role.

| Owner | Count | Roles / examples | Result |
| --- | ---: | --- | --- |
| `ProjectActivity.kt` | 10 | Archive rejection/confirmation; archive volume chooser; volume actions/move/remove/share; mismatch | Already styled; archive preview text shortened |
| `HomeProjectDialogController.kt` | 4 | Project actions, remove confirmation, messages | Already styled |
| `SettingsActivity.kt` | 3 | Mode, zoom and related choice dialogs | Already styled |
| `LifecycleHelpDialogController.kt` | 1 | Rotation-safe help | Already styled |
| `ViewerActivity.kt` | 2 | Section navigation and frame-debug report | Added shared styling |

## Canonical UI policy

- Use Android `AlertDialog.Builder(...).create()`, `show()`, then `DialogUi.apply(dialog, role)` for every app-owned dialog.
- Keep `DialogUi` as the source of visual truth: 16 dp rounded surface and border, consistent title/message text and secondary button styling.
- Use `DialogRole.HELP` for information, `CHOICE` for selections, `CONFIRM` for intentional affirmative actions, and `DANGER` for destructive actions; destructive affirmative actions stay red.
- Put **Cancel/Close** on the non-primary action; never turn Cancel into an operation.
- Keep the full original command/mutation contract. A style update must not move, delete, import, export, or reroute a volume.
- Give long technical or provenance messages a compact primary summary, with full details available on demand; preserve provenance in full.
- Never force one rigid button layout onto custom list/search content if that damages touch targets or accessibility.
- Preserve activity-specific restoration semantics. Styling is not lifecycle management; preview and help dialogs must retain their state across rotation without auto-running actions.

## UX finding: archive source preflight

**v0.5.80**: User phone QA confirmed `NT8341A` metadata duplicate detection with explicit project and SAF provider/Document ID, but all provenance text filled the default message and the software keyboard was visible underneath.

**v0.5.81 candidate**: The archive confirmation dialog now foregrounds source filename, target project and a compact possible-duplicate summary; `Технічні деталі ▼` expands the full provider, Document ID and registered-project NT match list **inside the same dialog**, with no third decision button. Expansion is saved during rotation. The existing Cancel/Continue callbacks and preflight identity check are preserved. No source is read, moved, deleted, or converted by expanding details.

## Explicit limitations and review gates

- Static source audit covers the **20 app-owned `AlertDialog` builders** identified in source, not Android-owned SAF/system windows and not every view designed to resemble a modal.
- UI screenshot/real-device QA remains required for width, small landscape screens, keyboard under alert, long scroll, touch targets, dialogs with dynamic custom content and expand/collapse restore.
- v0.5.80 Phone QA remains open. Rotation is confirmed PASS; explicit Cancel/no-operation after preview is not yet confirmed and must not be silently marked PASS.
- Cross-project NT match correctness is not proven by observing only a current-project match. Source guard claims metadata similarity, not matching ZIP/PDF hashes.
- No automatic deletion, source folder cleanup, or other user-data mutation is part of this UI work.

## Acceptance checklist

1. One visual baseline across Project, Home, Settings, Help and Viewer dialogs, with red destructive affirmative actions.
2. Archive preflight compact by default; full provider and Document ID accessible via `Технічні деталі`.
3. Expand details -> rotate -> same details expanded; collapse still works.
4. Cancel source preview -> no destination picker or new native run, and last diagnostic remains intact.
5. In Viewer, section navigator search and frame debug retain their current actions while matching dialog chrome.
6. Long names, many duplicate matches, keyboard presence, portrait and landscape all remain usable.
