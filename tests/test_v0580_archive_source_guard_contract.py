from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
D=ROOT/"android/app/src/main/java/com/saney/renaultdocs"

class V0580SafeArchiveContracts(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.p=(D/"ProjectActivity.kt").read_text(encoding="utf-8")
        cls.s=(D/"NativeRdpkgPreparationService.kt").read_text(encoding="utf-8")
        cls.g=(D/"ArchiveSourceGuard.kt").read_text(encoding="utf-8")
        cls.i=(D/"ArchiveIntake.kt").read_text(encoding="utf-8")
    def test_prepared_input_is_blocked_before_destination_or_staging(self):
        self.assertIn('ext == "rdpkg"',self.g)
        self.assertIn('setOf("zip", "7z", "rar")',self.g)
        self.assertIn("ArchiveSourceGuard.inputError(sourceName, project)",self.p)
        self.assertIn("ArchiveSourceGuard.inputError(request.sourceName, target)",self.s)
        picker=self.p.split("private fun startNativeArchiveRdpkgFlow()",1)[1].split("private fun openNativeRdpkgDestinationPicker",1)[0]
        self.assertNotIn('"application/octet-stream"',picker)
        self.assertNotIn("nativeRunStore.clearFinished()",picker)
    def test_user_confirms_exact_source_no_automatic_execution(self):
        self.assertIn("private fun showArchiveSourcePreflight()",self.p)
        self.assertIn("DocumentsContract.getDocumentId(source)",self.p)
        self.assertIn("Фактичний Document ID:",self.p)
        self.assertIn("pendingNativeSourceUri == source.toString()",self.p)
        self.assertIn("archiveSourcePreflightOpen",self.p)
        self.assertIn("clearPendingArchiveSource()",self.p)
        self.assertIn("DialogUi.apply(dialog, DialogRole.CONFIRM)",self.p)
    def test_global_catalog_and_model_contract(self):
        self.assertIn("store.projects().flatMap",self.g)
        self.assertIn("store.volumes(project.id)",self.g)
        self.assertIn("ArchiveSourceGuard.catalogMatches(",self.p)
        self.assertIn("не серед усіх файлів телефона",self.p)
        for word in ['"kangoo" to Regex','"megane" to Regex','"laguna" to Regex']:
            self.assertIn(word,self.g)
        self.assertIn("ArchiveSourceGuard.rootConflict(",self.s)
    def test_generated_dataset_flag_before_extraction(self):
        self.assertIn("preparedParents.isNotEmpty()",self.i)
        self.assertIn("require(!inspection.preparedPackageDetected)",self.s)
        self.assertLess(self.s.index("require(!inspection.preparedPackageDetected)"),self.s.index("stager.extract("))
    def test_no_mutation_of_originals_or_catalog(self):
        for phrase in ["deleteDocument(", "moveDocument(", "removeVolume(", "upsertVolume("]:
            self.assertNotIn(phrase,self.g)

if __name__=="__main__":
    unittest.main()
