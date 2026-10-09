# Renault Docs — volume/file/data deletion: source-only safety audit (2026-10-09)

**READ-ONLY audit, no phone delete action, no code changes.** Motivated by user question: if a volume/file exists in both Megane II and Kangoo II (possibly with similar filename), would removing one remove one file, two files or both sets of data? Exact existing filesystem contents / names on this particular phone were **not** inspected, so do not claim to know that these two records point to the same physical source.

## Separate resources

1. **Original source archive (.zip/.7z/.rar)** in Android Documents via SAF, e.g. the real NT8298A ZIP. Archive intake reads it and copies to temporary private staging; successful creation does **not** remove that source.
2. **Exported .rdpkg** saved to the user-selected SAF destination. This is an independent portable output file. Generating its canonical filename uses `RenaultVolumeIdentity.canonicalFileName(model = project.model, ...)`, which *begins with the project model*, so normally Megane II and Kangoo II filenames differ even for the same NT code/date. Do not conclude arbitrary existing or renamed files cannot collide without checking actual Document IDs.
3. **Installed app-private dataset payload** unpacked by `RdpkgImporter.install()` into `LocalDatasetDocumentsProvider.packageDirectory(context, packageId)` inside `context.noBackupFilesDir`; **packageId** from `NativeRdpkgPreparationEngine.packageId()` begins with `projectId` and the volume identity/date. The confirmed NT8298A result is `megane-ii-nt8298a-2005-11-28`. A separately generated Kangoo II version would normally be prefixed `kangoo-ii-...`, hence a distinct app-private directory.
4. **Project-volume association** in `ProjectStore` SharedPreferences. It stores `projectId`, `volumeId`, `treeUri` etc.; same display NT/date does not by itself mean identical installed payload or external file.
5. **Prepared share copy** in private `filesDir/prepared-share/volumes` for `Поділитися підготовленим .rdpkg`, optional. This is not the exported SAF .rdpkg or the installed dataset.

## Exact delete actions in current v0.5.87 code

| UI action | What the code removes | What stays |
|---|---|---|
| `Megane II → ⋮ тому → Видалити з проєкту` | The **single** matching `(projectId, volumeId)` association via `ProjectStore.removeVolume`. It also invalidates an existing *prepared project-share* `.rdproject` copy as project membership changed. | External source ZIP and exported .rdpkg, installed private dataset, other project records (incl. Kangoo II). Not a storage-space cleanup. |
| `⋮ тому → Видалити підготовлений .rdpkg` | The optional private *share cache* file(s) for the selected project/volume. `PreparedShareStore.deleteVolume` explicitly checks **canonical and legacy names**; thus it can remove **up to two private prepared-share filenames** if both exist. | Source archive, user-exported SAF .rdpkg, installed volume data and associations; other project's differently named cache should be separate. |
| `Видалити проєкт` (Home) | That project's record and its associations; invalidates its private prepared `.rdproject` share copy (canonical and legacy file names). | Source ZIP, user-exported SAF packages, installed app-private dataset files, **other projects' project records**. |
| Android **Settings → Apps → Renault Docs → Clear storage/data** | App-local data for the **entire application**, including SharedPreferences project associations and `noBackupFilesDir` installed payloads and private prepared-share cache — **not scoped to one project**. | Normally separate user Documents/SAF ZIP/RDPKG outputs, but do **not** test this to check sharing! Backups/data safety are a separate procedure. |
| Removing a single externally exported `.rdpkg` via system file manager | Exactly the selected filesystem/document item, as implemented by the external file manager; Renault Docs **does not** control this system operation. | Existing installed extracted payload may still work; if two project records reference the **same** document URI, deleting that shared external file can affect both external references. Actual URIs require direct read-only inspection; names alone cannot prove sharing. |

## Evidence in code (v0.5.87)

- `ProjectActivity.confirmRemoveVolume()` calls `store.removeVolume(project.id, volume.id)`, with UI explicitly saying `Файли на телефоні залишаться без змін`.
- `ProjectStore.removeVolume()` only removes matching pair from SharedPreferences; it calls `invalidatePreparedProject()` beforehand.
- `ProjectActivity.confirmDeletePreparedVolume()` calls `PreparedShareStore.deleteVolume()`, which iterates distinct canonical/legacy paths. No delete to `volume.treeUri` or SAF export URI.
- `HomeProjectDialogController.showRemoveConfirmation()` calls `ProjectStore.removeProject()`; its warning states originals remain.
- `NativeRdpkgPreparationService.processPreparedSource()`: output .rdpkg is written/validated, then `RdpkgImporter.install()` creates private dataset and `ProjectStore.upsertVolume()` registers it *before* COMPLETE.
- `RdpkgImporter.install()` uses packageId-specific private directory and may replace that **same packageId** on a reimport; this is a separate explicit import/update scenario, not the remove action.
- `RenaultVolumeIdentity.canonicalFileName()` prefixes `project.model`, and `NativeRdpkgPreparationEngine.packageId()` prefixes `projectId`. This protects **newly generated** projects' names but does not provide an exact inventory of manually renamed files, arbitrary SAF/legacy volumes or duplicate old content.

## Proposed next step (no mutation)

If the user wants a precise comparison for *two actual volumes*, use **read-only project volume information** (Megane II and Kangoo II: NT/documentCode, date, displayed title, file name, SAF source/reference and packageId if visible), preferably through an existing non-destructive diagnostics UI. Avoid blindly invoking any remove/clear-data button. Never infer physical identity solely from the displayed NT, source filename or an identical basename.

**User asked only to record/explain, not implement new deletion behavior and not delete files.**
