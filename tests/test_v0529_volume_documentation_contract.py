from pathlib import Path
import json
import tempfile
import unittest

from core.runtime_ir import _compile_volume_documentation
from core.runtime_ir_shards import write_runtime_ir_shards


def make_section(code: str, aide_target: str = "V/RUS/DOCUMENT/AIDE.PDF") -> dict:
    return {
        "code": code,
        "title": "Section " + code,
        "legacy_entrypoint": f"V/RUS/HTM/MENU/{code}.HTM",
        "compile_state": "section-ir-v2",
        "panels": [],
        "controls": [
            {
                "id": "menu-toolbar",
                "type": "action-bar",
                "panel_id": "menu",
                "items": [
                    {
                        "label": "GENE",
                        "action_id": "action-1",
                    },
                    {
                        "label": "AIDE",
                        "action_id": "action-2",
                    },
                ],
            }
        ],
        "actions": [
            {
                "id": "action-1",
                "type": "route",
                "route_type": "open-document",
                "source_panel_id": "menu",
                "label": "GENE",
                "target": "V/RUS/DOCUMENT/GENERAL.HTM",
                "document_id": "doc-general",
            },
            {
                "id": "action-2",
                "type": "route",
                "route_type": "open-document",
                "source_panel_id": "menu",
                "label": "AIDE",
                "target": aide_target,
                "document_id": "doc-aide",
            },
        ],
        "documents": [
            {
                "id": "doc-general",
                "type": "structured-html",
                "path": "V/RUS/DOCUMENT/GENERAL.HTM",
                "title": "General",
                "headings": [],
                "tables": [],
            },
            {
                "id": "doc-aide",
                "type": "pdf",
                "path": aide_target,
            },
        ],
        "assets": [],
        "source_files": [],
    }


class V0529VolumeDocumentationContractTests(unittest.TestCase):
    def test_identical_section_documentation_is_hoisted_once_per_volume(self):
        documentation = _compile_volume_documentation(
            [
                make_section("101"),
                make_section("105"),
                make_section("R325"),
            ]
        )

        self.assertIsNotNone(documentation)
        assert documentation is not None

        self.assertEqual("volume", documentation["scope"])
        self.assertEqual(3, documentation["verified_section_count"])
        self.assertEqual(
            ["GENE", "AIDE"],
            [item["label"] for item in documentation["menu_items"]],
        )

        action_ids = [
            item["action_id"]
            for item in documentation["menu_items"]
        ]
        self.assertTrue(
            all(value.startswith("vdoc-action-") for value in action_ids)
        )

        document_ids = {
            item["id"]
            for item in documentation["documents"]
        }
        self.assertIn("vdoc-document-doc-general", document_ids)
        self.assertIn("vdoc-document-doc-aide", document_ids)

    def test_conflicting_documentation_targets_are_not_hoisted(self):
        documentation = _compile_volume_documentation(
            [
                make_section("101"),
                make_section(
                    "105",
                    aide_target="V/RUS/DOCUMENT/OTHER-AIDE.PDF",
                ),
            ]
        )

        self.assertIsNone(documentation)

    def test_runtime_shards_write_one_volume_documentation_file(self):
        documentation = _compile_volume_documentation(
            [
                make_section("101"),
                make_section("105"),
            ]
        )
        assert documentation is not None

        runtime_tree = {
            "schema_version": 2,
            "compiler_phase": "section-ir-v2",
            "volumes": [
                {
                    "id": "volume-one",
                    "title": "Volume One",
                    "classic": {
                        "entrypoint": "V/INDEX.HTM",
                    },
                    "modern": {
                        "documentation": documentation,
                        "sections": [
                            make_section("101"),
                            make_section("105"),
                        ],
                    },
                }
            ],
        }

        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            package = root / "_renault"
            package.mkdir()

            index_path = write_runtime_ir_shards(
                runtime_tree=runtime_tree,
                package_root=package,
            )

            index = json.loads(
                index_path.read_text(encoding="utf-8")
            )
            volume = index["volumes"][0]

            documentation_path = volume["documentation_path"]
            self.assertTrue(
                documentation_path.startswith(
                    "_renault/runtime-ir/documentation/"
                )
            )

            shard = root / documentation_path
            self.assertTrue(shard.is_file())

            payload = json.loads(
                shard.read_text(encoding="utf-8")
            )
            self.assertEqual(
                "volume",
                payload["documentation"]["scope"],
            )
            self.assertEqual(
                2,
                payload["documentation"]["verified_section_count"],
            )

    def test_android_runtime_uses_volume_documentation_lazily(self):
        repo = Path(__file__).resolve().parents[1]

        reader = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/RuntimeIrReader.kt"
        ).read_text(encoding="utf-8")
        activity = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("volumeDocumentationPath", reader)
        self.assertIn("readVolumeDocumentation(", reader)
        self.assertIn('"documentation_path"', reader)

        self.assertIn("runtimeDocumentation", activity)
        self.assertIn("documentationLoading", activity)
        self.assertIn("openDocumentation(", activity)
        self.assertIn("readVolumeDocumentation(", activity)
        self.assertIn('"Відкриваю документацію тому…"', activity)

    def test_release_version(self):
        repo = Path(__file__).resolve().parents[1]
        gradle = (
            repo / "android/app/build.gradle.kts"
        ).read_text(encoding="utf-8")

        self.assertIn("versionCode = 66", gradle)
        self.assertIn('versionName = "0.5.50"', gradle)

    def test_fullscreen_button_has_visible_pressed_state(self):
        repo = Path(__file__).resolve().parents[1]

        pdf = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        ).read_text(encoding="utf-8")
        viewer = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        ).read_text(encoding="utf-8")

        self.assertIn('aria-pressed="false"', pdf)
        self.assertIn('#fullscreen[aria-pressed="true"]', pdf)
        self.assertIn("window.renaultSetFullscreen", pdf)
        self.assertIn("syncPdfFullscreenControl()", viewer)


if __name__ == "__main__":
    unittest.main()
