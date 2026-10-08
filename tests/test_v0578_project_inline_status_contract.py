from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"android/app/src/main/java/com/saney/renaultdocs"

class ProjectInlineStatusTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.p=(JAVA/"ProjectActivity.kt").read_text(encoding="utf-8")
        cls.s=(JAVA/"OperationStatusView.kt").read_text(encoding="utf-8")
        cls.home=(JAVA/"MainActivity.kt").read_text(encoding="utf-8")

    def test_status_is_inside_fixed_add_not_collapsible_body(self):
        panel=self.p[self.p.index("private fun buildAddPanel"):self.p.index("private fun addChoiceButton")]
        self.assertIn("addView(\n                operationStatus",panel)
        self.assertLess(panel.index("addView(\n                addPanelBody"),panel.index("operationStatus ="))
        self.assertIn("root.addView(\n            buildAddPanel()",self.p)
        self.assertIn("projectScroll.addView(\n            volumeContainer",self.p)

    def test_progress_cancel_close_copy_are_accessible(self):
        for text in ["fun useProjectCompactLayout(","fun isProjectDetailsExpanded()","detailsToggleView","copyButtonView","Копіювати повний статус","progressView.visibility = View.VISIBLE","onCancel()","onClose()","updateProjectCompactUi()"]:
            self.assertIn(text,self.s)

    def test_rotation_restore_without_relaunch(self):
        for text in ["STATE_OPERATION_STATUS_DETAILS_EXPANDED","operationStatus.isProjectDetailsExpanded()","restoredStatusDetailsExpanded","useProjectCompactLayout("]:
            self.assertIn(text,self.p)

    def test_home_status_stays_original(self):
        self.assertIn("operationStatus = OperationStatusView(this)",self.home)
        self.assertNotIn("useProjectCompactLayout(",self.home)

if __name__=="__main__":
    unittest.main()
