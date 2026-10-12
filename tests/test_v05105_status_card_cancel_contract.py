"""v0.5.105: «Скасувати» on every long-operation status card (faric, 2026-10-12).

Phone QA: the Home card «Підготовка проєкту» had «Скасувати» (photo 2), the
project-screen card «Підготовка тому» did not (photo 1). Owner asked for the
same «Скасувати» wherever something is prepared, exported or imported.

Added (each asks first via ConfirmActions.confirmStop):
- «Підготовка тому» (RdpkgShareService) — unfinished file stays in .incoming
  and is removed; no ⇧ appears.
- «Експорт тому» (RdpkgExportService) — the unfinished .rdpkg this export
  created is removed.
- «Імпорт тому» (RdpkgImportService) — stops only at the importer's safe
  checkpoints; the activation rename is never interrupted.
- «Створення тому» — also during «Перевіряю/встановлюю» (IMPORTING).
- «Архів · вибір томів» card.
- Google Drive catalog download/import (CatalogImportService) — volumes
  already added stay; an installed volume is always registered.
Moving a volume is instant (metadata only), nothing to cancel.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def read(name):
    return (ANDROID / name).read_text(encoding="utf-8")


def between(text, start, end):
    assert start in text, start
    rest = text.split(start, 1)[1]
    assert end in rest, end
    return rest.split(end, 1)[0]


STORES = {
    "RdpkgShareRunStore.kt": "RdpkgShareRunPhase",
    "RdpkgExportRunStore.kt": "RdpkgExportRunPhase",
    "RdpkgImportRunStore.kt": "RdpkgImportRunPhase",
    "CatalogImportRunStore.kt": "CatalogImportPhase",
}


PROGRESS_CHECK = (
    "progressState = { progress ->\n"
    "                        dataSyncTimeout.checkActive()\n"
    "                        if (runStore.isCancelRequested()) throw OperationCancelledException()"
)


class StatusCardCancelContract(unittest.TestCase):
    def test_stores_persist_cancel_and_cancelled_is_terminal(self):
        for name, enum in STORES.items():
            store = read(name)
            with self.subTest(store=name):
                self.assertIn("CANCELLED", between(store, f"enum class {enum}", "}"))
                self.assertIn(f"{enum}.CANCELLED", between(store, "isTerminal", "\n\n"))
                self.assertIn("fun requestCancel(): Boolean", store)
                self.assertIn("if (!load().isRunning) return false", store)
                self.assertIn("fun isCancelRequested(): Boolean", store)
                self.assertIn("fun cancelled(message: String)", store)
                # A new run starts clean: begin() clears every key, including the request.
                self.assertIn(".clear()", between(store, "fun begin(", "fun "))

    def test_volume_prepare_worker_stops_and_never_commits(self):
        service = read("RdpkgShareService.kt")
        self.assertIn(PROGRESS_CHECK, service)
        self.assertGreaterEqual(service.count("if (runStore.isCancelRequested()) throw OperationCancelledException()"), 2)
        cancel_check = service.index("if (runStore.isCancelRequested()) throw OperationCancelledException()\n                // ⇧")
        self.assertLess(cancel_check, service.index("PreparedShareStore.commitIncoming(partial, file)"))
        self.assertIn('runStore.cancelled("Підготовку тому скасовано.', service)
        self.assertIn("temp?.let { runCatching { it.delete() } }", service)

    def test_volume_export_worker_removes_only_its_unfinished_file(self):
        service = read("RdpkgExportService.kt")
        # The check sits inside the exporter's progress callback (a bad edit
        # once broke this call in CI — v0.5.105 history).
        self.assertIn(PROGRESS_CHECK, service)
        self.assertGreaterEqual(service.count("if (runStore.isCancelRequested()) throw OperationCancelledException()"), 2)
        cancelled = between(service, "if (error is OperationCancelledException", "val message =")
        self.assertIn("DocumentsContract.deleteDocument(contentResolver, Uri.parse(destinationUri))", cancelled)
        self.assertIn("Недороблений файл .rdpkg видалено", cancelled)
        self.assertIn("не вдалося видалити — видали його вручну", cancelled)

    def test_volume_import_uses_safe_importer_checkpoints(self):
        service = read("RdpkgImportService.kt")
        self.assertIn("isCancelled = { dataSyncTimeout.isExpired || runStore.isCancelRequested() }", service)
        self.assertIn('runStore.cancelled("Імпорт тому скасовано.', service)
        importer = read("RdpkgImporter.kt")
        # The importer's last checkpoint is before the activation transaction.
        self.assertLess(
            importer.index("// Last cancellation checkpoint before atomic rename/rollback."),
            importer.index("staging.renameTo("),
        )

    def test_catalog_import_keeps_added_volumes_and_registers_installed_one(self):
        service = read("CatalogImportService.kt")
        self.assertIn("isCancelled = { dataSyncTimeout.isExpired || runStore.isCancelRequested() }", service)
        loop = between(service, "items.forEachIndexed {", "dataSyncTimeout.checkActive()\n        runStore.complete(")
        self.assertIn("if (runStore.isCancelRequested()) throw OperationCancelledException()", loop)
        # After install there is no cancel checkpoint before upsertVolume.
        after_install = between(loop, ".getOrThrow()", "store.upsertVolume(")
        self.assertNotIn("isCancelRequested", after_install)
        self.assertIn("importedCount++", loop)
        cancelled = between(service, "if (runStore.isCancelRequested() &&", "val message =")
        self.assertIn("Уже додано: $done", cancelled)
        self.assertIn('File(cacheDir, "catalog-downloads")', cancelled)

    def test_project_cards_offer_cancel_with_question(self):
        project = read("ProjectActivity.kt")
        for title, store, question in (
            ('title = "Імпорт тому"', "rdpkgImportRunStore", "Зупинити імпорт тому?"),
            ('title = "Експорт тому"', "rdpkgExportRunStore", "Зупинити експорт тому?"),
            ('title = "Підготовка тому"', "rdpkgShareRunStore", "Зупинити підготовку тому?"),
        ):
            with self.subTest(card=title):
                start = project.index(title)
                card = project[start - 200:start + 1800]
                self.assertIn(f"{store}.isCancelRequested()", card)
                self.assertIn("onCancel =", card)
                self.assertIn("projectConfirms.show(CONFIRM_STOP_", card)
                # While stopping, the button hides and the card says so.
                self.assertIn("STOPPING_DETAIL", card)
                # v0.5.106: the question lives in the keyed builder.
                builder = between(project, "private fun buildProjectConfirm(key: String)", "private fun buildStopOperationDialog(")
                self.assertIn(f'"{question}"', builder)
                self.assertIn(f"{store}.requestCancel()", builder)
        helper = between(project, "private fun buildStopOperationDialog(", "private fun confirmCancelArchiveSelection()")
        self.assertIn("ConfirmActions.confirmStop(", helper)
        self.assertIn("if (!running) return null", helper)

    def test_project_cards_show_cancelled_result(self):
        project = read("ProjectActivity.kt")
        for phase, title in (
            ("RdpkgImportRunPhase.CANCELLED", '"Імпорт тому · скасовано"'),
            ("RdpkgExportRunPhase.CANCELLED", '"Експорт тому · скасовано"'),
            ("RdpkgShareRunPhase.CANCELLED", '"Підготовка тому · скасовано"'),
        ):
            with self.subTest(phase=phase):
                self.assertIn(phase, project)
                self.assertIn(title, project)
        # A cancelled volume prepare never opens the share chooser.
        share = between(project, "private fun refreshRdpkgShareRunState()", "private fun sharePreparedRdpkg(")
        cancelled = between(share, "RdpkgShareRunPhase.CANCELLED", "} else {")
        self.assertNotIn("sharePreparedRdpkg", cancelled)

    def test_native_and_archive_cards_offer_cancel(self):
        project = read("ProjectActivity.kt")
        self.assertIn("if (state.isRunning && !state.cancelRequested) {", project)
        archive = between(project, '"Архів · вибір томів"', "showArchiveVolumeChooser(")
        self.assertIn("confirmCancelArchiveSelection()", archive)
        helper = between(project, "private fun buildCancelArchiveSelectionDialog()", "    /**")
        self.assertIn('"Скасувати вибір томів?"', helper)
        after_yes = helper.split('"Скасувати вибір томів?"', 1)[1]
        self.assertLess(after_yes.index(".requestCancel("), after_yes.index("?.dismiss()"))

    def test_catalog_card_offers_cancel_and_cancelled_title(self):
        drive = read("DriveCatalogActivity.kt")
        self.assertIn("importStore.isCancelRequested()", drive)
        self.assertIn("confirmCancelImport()", drive)
        self.assertIn('"Зупинити імпорт із каталогу?"', drive)
        self.assertIn('"Імпорт із каталогу · скасовано"', drive)

    def test_version_bumped(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        code = int(gradle.split("versionCode = ", 1)[1].splitlines()[0].strip())
        self.assertGreaterEqual(code, 121)


if __name__ == "__main__":
    unittest.main()
