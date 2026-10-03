from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


class DurableRdpkgImportContractTest(unittest.TestCase):
    def test_direct_rdpkg_import_is_not_activity_owned(self):
        project = read("ProjectActivity.kt")
        start = project.index("private fun handleRdpkgResult")
        end = project.index("private fun handleRdpkgExportResult", start)
        flow = project[start:end]
        self.assertNotIn("Thread {", flow)
        self.assertNotIn("RdpkgImporter.install(", flow)
        self.assertIn("RdpkgImportService.start(", flow)

    def test_rdpkg_import_has_durable_run_store_and_service(self):
        service = read("RdpkgImportService.kt")
        store = read("RdpkgImportRunStore.kt")
        manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn("RdpkgImporter.install(", service)
        self.assertIn("startForeground(", service)
        self.assertIn("RdpkgImportRunStore", service)
        self.assertIn("consumedFinishedAtMs", store)
        self.assertIn("fun consume(", store)
        self.assertIn('android:name=".RdpkgImportService"', manifest)
        self.assertIn('android:stopWithTask="false"', manifest)

    def test_project_reattaches_and_consumes_terminal_import_once(self):
        project = read("ProjectActivity.kt")
        self.assertIn("refreshRdpkgImportRunState()", project)
        self.assertIn("rdpkgImportRunStore.consume(state.finishedAtMs)", project)
        self.assertIn("LocalDatasetDocumentsProvider.treeUriFor(packageId)", project)
        self.assertIn("importPreparedVolume(", project)
        self.assertIn("allowOverride = true", project)


if __name__ == "__main__":
    unittest.main()
