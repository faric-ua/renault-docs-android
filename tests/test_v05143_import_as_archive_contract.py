"""v0.5.143 — archives plan v2, stage 2 (owner 2026-10-17 03:10: «давай етап 2») + TODO №19.

Stage 2: an imported `.rdpkg` (single file, `.rdproject`, «Готові проєкти») is copied as one
file next to the volumes folder, checked entry by entry, and only then gets its final name;
the volume is read from it. No thousands of small files — 18 volumes on the M30s card took
~25 hours unpacked. Nothing installed is deleted; an older archive of the same volume is set
aside and put back on failure. A volume that lies unpacked keeps the old replace path.

TODO №19: on the M30s the compressor sometimes wrote a broken ZIP. Every exported volume,
«Поділитися томом» and `.rdproject` is unpacked again while it is written (ZipStreamCheck);
a broken file fails the export and only that file is removed.
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


class ImportAsArchiveContract(unittest.TestCase):
    def test_version(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        code = int(gradle.split("versionCode = ")[1].split()[0])
        self.assertGreaterEqual(code, 159)

    def test_import_goes_through_the_archive_first(self):
        importer = source("RdpkgImporter.kt")
        self.assertIn("const val IMPORT_AS_ARCHIVE =\n        true", importer)
        install = between(importer, "    fun install(", "    private fun installAsArchive(")
        archive_at = install.index("installAsArchive(")
        staging_at = install.index("val staging =")
        self.assertLess(archive_at, staging_at, "archive import is tried before unpacking")
        self.assertIn("?.let { return@runCatching it }", install)

    def test_archive_install_checks_before_naming(self):
        importer = source("RdpkgImporter.kt")
        body = between(importer, "    private fun installAsArchive(", "    private fun copyPackage(")
        nomedia = body.index("VolumeStorage.ensureNoMedia(archiveDirectory)")
        part = body.index('".import-"')
        self.assertLess(nomedia, part, ".nomedia before the first volume file")
        check = body.index("VolumePackageCheck.check(")
        rename = body.index("part.renameTo(target)")
        self.assertLess(check, rename, "final name only after every entry was checked")
        self.assertIn("throw PackageAlreadyInstalledException(packageId)", body)
        self.assertIn("if (previousFolder != null) return null", body, "unpacked volume: old replace path")
        self.assertIn("backup.renameTo(previousArchive)", body, "older archive goes back on failure")
        self.assertIn("if (part.exists()) part.delete()", body, "only its own unfinished file")
        self.assertNotIn("deleteRecursively", body, "no folder is ever removed here")
        self.assertIn("VolumeArchiveAccess.setReadingOff(context, packageId, off = false)", body)

    def test_check_progress_is_throttled(self):
        # v0.5.144, M30s 2026-10-17 03:48: «до 50 % швидко, а потім довго» — every entry
        # reported progress (run state + notification). Now like unpacking: ≤ every 100 ms.
        importer = source("RdpkgImporter.kt")
        body = between(importer, "    private fun installAsArchive(", "    private fun copyPackage(")
        self.assertIn("now - lastCheckProgressAt < PROGRESS_THROTTLE_MS", body)

    def test_installed_means_folder_or_archive(self):
        storage = source("VolumeStorage.kt")
        self.assertIn('const val ARCHIVE_DIR = "rdpkg-archive"', storage)
        state = between(storage, "    fun state(", "    fun chosenPlace(")
        self.assertIn("archiveIn(phoneRoot, packageId).isFile", state)
        self.assertIn("archiveIn(it, packageId).isFile", state)
        access = source("VolumeArchiveAccess.kt")
        self.assertIn("if (folder != null && isReadingOff(appContext, packageId))", access)
        self.assertIn("VolumeStorage.locateArchive(context, packageId)", access)

    def test_menu_shows_one_file_volume(self):
        activity = source("ProjectActivity.kt")
        self.assertIn("VolumeArchiveAccess.isArchiveOnly(this, archivePackageId)", activity)
        self.assertIn("Том — один файл-архів ✓", activity)

    def test_export_of_one_file_volume_copies_the_archive(self):
        exporter = source("RdpkgExporter.kt")
        export = between(exporter, "    fun export(", "    fun exportArchive(")
        self.assertIn("VolumeArchiveAccess.active(appContext, packageId)", export)
        self.assertIn("copyArchive(", export)
        copy = between(exporter, "    private fun copyArchive(", "    private fun validateManagedPackage(")
        self.assertIn("ZipStreamCheck()", copy)
        self.assertIn("check.finish(expectedFiles = archive.fileCount)", copy)

    def test_todo19_every_zip_writer_checks_its_bytes(self):
        writer = source("RdpkgZipWriter.kt")
        self.assertIn("ZipStreamCheck()", writer)
        self.assertIn("check.output", writer)
        self.assertIn("check.finish(", writer)
        project = source("RdprojectExporter.kt")
        self.assertIn("ZipStreamCheck().use { check ->", project)
        self.assertIn("projectFiles.size", between(project, "check.finish(", ")"))
        service = source("RdpkgExportService.kt")
        self.assertIn("error is DamagedOutputException", service)
        damaged = between(service, "error is DamagedOutputException", "return@Thread")
        self.assertIn("DocumentsContract.deleteDocument(contentResolver, Uri.parse(destinationUri))", damaged)
        check = source("ZipStreamCheck.kt")
        self.assertIn("Сам том не змінено.", check)


    def test_v05145_no_compressor_where_it_breaks(self):
        # Phone QA M30s 2026-10-17 04:24: ZipStreamCheck caught «invalid code lengths set» in
        # «Підготувати і поділитися томом» NT8228A. Android 11 (or a phone where it once broke)
        # writes packages STORED; other phones keep the compressor (same bytes as on Drive).
        policy = source("ZipCompressionPolicy.kt")
        self.assertIn("Build.VERSION.SDK_INT <= Build.VERSION_CODES.R", policy)
        self.assertIn("fun rememberBroken(", policy)
        writer = source("RdpkgZipWriter.kt")
        self.assertIn("if (ZipCompressionPolicy.storedOnly(context))", writer)
        self.assertIn("private fun writeStored(", writer)
        self.assertIn("VolumeArchiveWriter.writeTo(", writer)
        self.assertIn("ZipCompressionPolicy.rememberBroken(context)", writer)
        project = source("RdprojectExporter.kt")
        self.assertIn("val storedOnly = ZipCompressionPolicy.storedOnly(context)", project)
        self.assertIn("method = ZipEntry.STORED", project)
        self.assertIn("ZipCompressionPolicy.rememberBroken(context)", project)
        self.assertIn("fun writeTo(", source("VolumeArchiveWriter.kt"))


if __name__ == "__main__":
    unittest.main()
