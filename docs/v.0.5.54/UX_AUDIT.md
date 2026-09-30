# Renault Docs v0.5.54 — UI / lifecycle audit

Date: 2026-09-30

## User-requested copy / layout

Accepted target:

- Home primary action:
  - title: `Додати том`
  - subtitle: `До проєкту`
- Empty project card:
  - `Порожній · додай том`
- Project add area:
  - one parent tile: `Додати`
  - child actions: `Авто` and `Вручну`
- `Авто` = recommended single-volume `.rdpkg` import.
- `Вручну` = prepared folder / SAF compatibility path.
- raw → `.rdpkg` stays a separate explicit operation.

## Help-window contract

New shared `LifecycleHelpDialogController`:

- Help is identified by a stable screen-local ID;
- open Help ID is saved in `savedInstanceState`;
- rotation restores the same Help above the same Activity;
- restoring Help never triggers the underlying action;
- closing Help returns to the same screen;
- Help dismissal during configuration change does not clear the restored state;
- Help uses one consistent close action: `Зрозуміло`;
- Help uses Renault Docs dark surface/border/accent styling instead of the default system-gray dialog.

Help is added only where the operation is meaningfully ambiguous:

| Surface | Help | Reason |
| --- | --- | --- |
| Home / Library | Yes | project vs volume, New Project, service tools |
| Project | Yes | project/volume model |
| Project → Add | Yes | Auto vs Manual |
| Project → raw → rdpkg | Yes | source/destination/background behavior |
| Converter | Yes | source, destination, merge, validation |
| Modern volume | Yes | Modern vs Classic vs Documentation |
| Native section | Yes | Runtime IR actions, Classic fallback, opaque IDs |
| Volume documentation | Yes | native panels and nested navigation |
| Project chooser | No | single obvious choice |
| Create project | Inline explanation | single text field/action |
| Settings | Existing labels sufficient | chooser modals already lifecycle-safe |
| Viewer | Existing controls sufficient | already has dedicated state/reconcile logic |

## Rotation / lifecycle audit

### Already safe before v0.5.54

- Settings chooser dialogs persist an `activeDialogKind` and reopen after rotation.
- Viewer persists dialog kind, PDF/search/split state and handles configuration changes.
- Native raw → `.rdpkg` preparation is service/store-backed; Activity recreation reattaches instead of restarting the operation.
- ModernVolume persists search query.
- NativeSection persists current view/menu/PDF export state.
- VolumeDocumentation persists its panel stack.
- Conversion uses persistent draft/run stores rather than Activity ownership.

### Fixed in v0.5.54

- Help windows use one shared rotation-safe contract.
- CreateProject preserves the typed project name on rotation.
- Project `pendingManualImport` survives Activity recreation.
- Project modal states are restorable:
  - volume actions;
  - remove-volume confirmation;
  - multi-volume chooser;
  - wrong-project confirmation.
- Project modal restore is presentation-only:
  - no export;
  - no delete;
  - no add/import;
  - no picker is launched automatically.
- Multi-volume/wrong-project restore re-reads the already-permitted tree URI only to rebuild the same confirmation UI.

### Follow-up found by the audit

**RISK-LIFE-001 — direct .rdpkg install worker is still Activity-owned.**

`ProjectActivity.handleRdpkgResult()` starts a plain Thread for `RdpkgImporter.install()`.
A rotation/process recreation during the actual package copy can outlive the old Activity and lose the final UI/upsert handoff.

This is not a Help/modal defect and existing completed imports remain valid, but the durable lifecycle target should be:
package install service/run-store → validation → project upsert → Activity reattach.

Do not hide this finding by adding another dialog-state flag.

## Panels / navigation

- VolumeDocumentation nested panel stack is already restored after rotation.
- NativeSection restores its current native view/panel/document identity.
- ModernVolume search is restored.
- Help overlays are now restored independently above those same states.
- Back closes Help first through the platform dialog behavior; after Help closes the underlying screen/panel remains unchanged.

## Accessibility / button audit

- Help has a consistent `Довідка` content description.
- Main Settings keeps its explicit content description.
- Project add actions are separate focusable controls.
- destructive volume removal remains behind explicit confirmation.
- terminal-status `×` remains terminal-only and separate from Help.

## v0.5.54 phone gates

1. Home:
   - `Додати том / До проєкту`;
   - empty Kangoo: `Порожній · додай том`.
2. Project:
   - one `Додати` tile;
   - `Авто` + `Вручну`;
   - raw builder remains separate.
3. Help rotation:
   - open Home Help → rotate → same Help stays open;
   - close → same Home state;
   - open Project Add Help → rotate → same Help stays open;
   - no picker/import starts.
4. Project dialog rotation:
   - open `Дії тому` → rotate → dialog restores;
   - open remove confirmation → rotate → confirmation restores;
   - no export/delete occurs automatically.
5. Create project:
   - type a name → rotate → text remains.
6. Existing project/data:
   - Laguna II volumes stay present;
   - representative NT8183A and NT8328A still open.


## Compact action subtitle typography

After phone review, secondary labels inside action tiles are standardized through one UI token:
`Ui.actionSubtitleSp = 11sp`.

Applied to:
- Home primary action subtitles;
- Home service/tool subtitles;
- Project `Авто` / `Вручну` subtitles;
- Project standalone action-card subtitles such as raw → `.rdpkg`.

Primary action titles remain unchanged. This avoids manual line breaks and keeps the same hierarchy across cards.


## Settings typography

Phone review expanded the compact typography rule to Settings:
- section titles and setting names remain prominent;
- descriptions use the shared secondary 12sp token;
- selected values use the shared 13sp value token;
- small labels/warnings use the compact 11sp token;
- Backup action buttons use a shared compact 12sp button token with Renault Docs surface/border styling;
- section label `Backup` is localized to `Резервні копії`.
