"""v0.5.141 — archives plan v2, stage 1 (owner 2026-10-17 00:32: «починай перший етап»).

A volume can be read straight from its one `.rdpkg` file. «Зробити архів тому» packs the
installed folder next to the volumes folder, checks every file, and only then the volume is
read from the archive. Nothing is deleted: the folder stays, the switch goes both ways.
"""

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def source(name: str) -> str:
    return (SRC / name).read_text(encoding="utf-8")


def between(text: str, start: str, end: str) -> str:
    a = text.index(start)
    return text[a:text.index(end, a)]


class VolumeArchiveContract(unittest.TestCase):
    def test_both_read_points_know_the_archive(self):
        provider = source("LocalDatasetDocumentsProvider.kt")
        self.assertIn("VolumeArchiveAccess.active(appContext, packageId)", provider)
        self.assertIn("archive.children(parent.relativePath)", provider)
        self.assertIn("private fun openFromArchive(", provider)
        self.assertIn("ParcelFileDescriptor.createReliablePipe()", provider)
        self.assertIn("closeWithError(", provider, "a broken read is an error, not a cut file")
        resolver = source("SafDatasetResolver.kt")
        self.assertIn("VolumeArchiveAccess.active(appContext, packageId)", resolver)
        self.assertIn("archiveStream(node)", resolver)
        self.assertIn("archiveRealFile(node)", resolver)
        self.assertIn("fun ownAttributes(relativePath: String)", resolver)

    def test_pdf_needs_a_real_file_from_the_phone_cache(self):
        cache = source("VolumeArchiveFileCache.kt")
        self.assertIn('DIR_NAME = "renault-archive-files"', cache)
        self.assertIn("DEFAULT_LIMIT_BYTES = 300L * 1024 * 1024", cache)
        self.assertIn('f.name.endsWith(SUFFIX)', cache, "trim touches only its own copies")
        access = source("VolumeArchiveAccess.kt")
        self.assertIn("context.applicationContext.cacheDir, VolumeArchiveFileCache.DIR_NAME", access)
        pdf = source("AndroidPdfLayer.kt")
        self.assertIn("resolver.ownAttributes(path)", pdf, "drawn pages keyed for archive volumes too")

    def test_archive_is_used_only_when_checked_and_matching(self):
        access = source("VolumeArchiveAccess.kt")
        # v0.5.143 (archives stage 2): the folder name moved to VolumeStorage, shared with the
        # importer that keeps imported volumes as one archive file; the name did not change.
        self.assertIn("const val DIR = VolumeStorage.ARCHIVE_DIR", access)
        self.assertIn('const val ARCHIVE_DIR = "rdpkg-archive"', source("VolumeStorage.kt"))
        self.assertIn('const val PART_SUFFIX = ".rdpkg.part"', access)
        self.assertIn("sameManifest(folder, archive)", access, "volume installed again → old archive not used")
        self.assertIn("isReadingOff(appContext, packageId)", access)
        self.assertIn("VolumeFileIndex.onForget", access)
        self.assertNotIn(".delete()", access, "nothing is deleted when choosing where to read")
        self.assertNotIn("deleteRecursively", access)

    def test_making_the_archive_checks_every_file_first(self):
        service = source("RdpkgExportService.kt")
        finish = between(service, "private fun finishArchive(", "override fun onTimeout(")
        verify_at = finish.index("VolumeArchiveVerifier.verify(")
        rename_at = finish.index("part.renameTo(target)")
        self.assertLess(verify_at, rename_at, "final name only after the check")
        self.assertIn("if (!check.ok)", finish)
        self.assertIn("VolumeFileIndex.forget(result.packageId)", finish)
        self.assertNotIn("deleteRecursively", finish, "the unpacked folder is never touched")
        self.assertIn("папка тому лишилась як була", finish)
        # Cancel / failure: only this run's own unfinished archive file goes.
        self.assertIn("val archiveMode = VolumeArchiveAccess.isArchivePart(destinationUri)", service)
        self.assertIn("Недороблений архів видалено, сам том не змінено.", service)
        verifier = source("VolumeArchiveVerifier.kt")
        self.assertIn("ZipInputStream", verifier)
        self.assertIn("CRC32", verifier)
        self.assertIn("В архіві є зайвий файл", verifier)
        self.assertIn("В архіві бракує", verifier)

    def test_volume_menu_and_nomedia_first(self):
        project = source("ProjectActivity.kt")
        menu = between(project, "private fun showVolumeActions(", "private fun showMoveVolumeDialog(")
        self.assertIn("Зробити архів тому (перевірка, нічого не видаляє)", menu)
        self.assertIn("Читати з папки (архів лишиться)", menu)
        self.assertIn('"Читати з архіву"', menu)
        self.assertIn("Архів тому застарів — зробити заново", menu)
        self.assertIn("ScrollView(this).apply { addView(actions) }", menu)
        start = between(project, "private fun startVolumeArchive(", "private fun startRdpkgExport(")
        self.assertLess(start.index("VolumeStorage.ensureNoMedia(folder)"), start.index("RdpkgExportService.start("))
        self.assertIn("Uri.fromFile(part)", start)
        refresh = between(project, "private fun refreshRdpkgExportRunState()", "Read persisted source provenance")
        self.assertIn('"Архів тому готовий"', refresh)

    def test_archive_written_stored_without_compressor(self):
        # v0.5.142 (M30s 2026-10-17 01:22–01:39): DEFLATE written on Android 11 came out
        # broken (different zlib error each time) — the archive uses no compressor at all.
        writer = source("VolumeArchiveWriter.kt")
        self.assertIn("zip.setMethod(ZipOutputStream.STORED)", writer)
        self.assertIn("method = ZipEntry.STORED", writer)
        self.assertNotIn("setLevel", writer)
        self.assertNotIn("Deflater", writer)
        exporter = source("RdpkgExporter.kt")
        self.assertIn("fun exportArchive(", exporter)
        self.assertIn("VolumeArchiveWriter.write(", exporter)
        service = source("RdpkgExportService.kt")
        self.assertIn("RdpkgExporter.exportArchive(", service)
        verifier = source("VolumeArchiveVerifier.kt")
        self.assertIn("Архів пошкоджений у файлі $path", verifier)

    def test_read_log_and_diagnostics_legend(self):
        access = source("VolumeArchiveAccess.kt")
        self.assertIn('"ARCHIVE том=', access)
        self.assertIn('"ARCHIVE-PDF том=', access)
        menu = (ROOT / "tools/termux/phone-diagnostics-menu.sh").read_text(encoding="utf-8")
        self.assertIn("ARCHIVE — том читається з архіву", menu)

    def test_version_and_guide(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        version = int(gradle.split("versionCode = ", 1)[1].split()[0])
        self.assertGreaterEqual(version, 157)
        guide = (ROOT / "docs/ua/USER_GUIDE.md").read_text(encoding="utf-8")
        self.assertIn("Зробити архів тому", guide)
        self.assertIn("Читати з папки", guide)


if __name__ == "__main__":
    unittest.main()
