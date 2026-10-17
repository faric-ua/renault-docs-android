import hashlib
import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest import mock
from zipfile import ZIP_DEFLATED, ZIP_STORED, ZipFile, ZipInfo

from tools import fix_volume_logo as fx
from tools.validate_rdpkg_payload import validate_rdpkg_payload

BAD = b"GIF89a-bad-picture"
GOOD = b"GIF89a-renodocs-logo-new"
OTHER = b"GIF89a-other-icon"


def zinfo(name, method=ZIP_DEFLATED):
    info = ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
    info.compress_type = method
    info.external_attr = 0o644 << 16
    return info


def make_fast_pack(files):
    buf = io.BytesIO()
    with ZipFile(buf, "w") as z:
        for name, data in files.items():
            z.writestr(zinfo(name), data)
    return buf.getvalue()


def make_rdpkg(path, with_fast=True, logo=BAD, method=ZIP_DEFLATED):
    tree = {
        "laguna/INDEX.HTM": b"<img src='ICONES/LOGO.GIF'>",
        "laguna/ICONES/LOGO.GIF": logo,
        "laguna/ICONES/LOGO_OLD.GIF": OTHER,
    }
    entries = dict(tree)
    dataset = {"project_id": "laguna-2", "entrypoint": "laguna/INDEX.HTM", "volumes": [{"id": "NT8183A"}]}
    if with_fast:
        fast = make_fast_pack(tree)
        sha = hashlib.sha256(fast).hexdigest()
        fast_name = "_renault/fast-content-" + sha[:16] + ".zip"
        entries[fast_name] = fast
        dataset["fast_pack"] = {"schema_version": 1, "format": "zip-web-v1", "path": fast_name, "sha256": sha,
                                "bytes": len(fast), "file_count": len(tree)}
    entries["renault-dataset.json"] = json.dumps(dataset, indent=2).encode()
    manifest = {
        "schema_version": 1, "format": "renault-volume-package-v1", "package_id": "laguna-2-nt8183a",
        "project_id": "laguna-2", "volume": {"id": "NT8183A", "entrypoint": "laguna/INDEX.HTM"},
        "dataset_manifest": "renault-dataset.json",
        "payload_file_count": len(entries), "payload_bytes": sum(len(v) for v in entries.values()),
    }
    with ZipFile(path, "w") as z:
        z.writestr(zinfo("rdpkg.json", method), json.dumps(manifest, indent=2))
        for name, data in entries.items():
            z.writestr(zinfo(name, method), data)


class FixVolumeLogoTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.patches = [
            mock.patch.object(fx, "BAD_SIZE", len(BAD)),
            mock.patch.object(fx, "BAD_SHA256", hashlib.sha256(BAD).hexdigest()),
        ]
        for p in self.patches:
            p.start()
        self.logo = self.root / "logo.gif"
        self.logo.write_bytes(GOOD)

    def tearDown(self):
        for p in self.patches:
            p.stop()
        self.tmp.cleanup()

    def all_files(self, path):
        out = {}
        with ZipFile(path) as z:
            for info in z.infolist():
                data = z.read(info)
                out[info.filename] = data
                if fx.is_nested_zip(info.filename):
                    with ZipFile(io.BytesIO(data)) as inner:
                        for i in inner.infolist():
                            out[info.filename + "!" + i.filename] = inner.read(i)
        return out

    def test_rdpkg_tree_and_fast_pack_fixed_and_valid(self):
        src = self.root / "NT8183A.rdpkg"
        make_rdpkg(src)
        original = src.read_bytes()
        self.assertEqual(fx.count_bad(src), 2)

        target, report = fx.fix_file(src, GOOD)

        self.assertEqual(report.count, 2)
        self.assertEqual(src.read_bytes(), original, "original must stay untouched")
        self.assertEqual(target, self.root / "logo-fixed" / "NT8183A.rdpkg")
        files = self.all_files(target)
        self.assertEqual(files["laguna/ICONES/LOGO.GIF"], GOOD)
        self.assertEqual(files["laguna/ICONES/LOGO_OLD.GIF"], OTHER)
        self.assertNotIn(BAD, files.values())
        fast_names = [n for n in files if n.endswith(".zip")]
        self.assertEqual(len(fast_names), 1)
        self.assertEqual(files[fast_names[0] + "!laguna/ICONES/LOGO.GIF"], GOOD)
        dataset = json.loads(files["renault-dataset.json"])
        self.assertEqual(dataset["fast_pack"]["path"], fast_names[0])
        self.assertEqual(dataset["fast_pack"]["sha256"], hashlib.sha256(files[fast_names[0]]).hexdigest())
        self.assertTrue(fast_names[0].endswith(dataset["fast_pack"]["sha256"][:16] + ".zip"))
        with ZipFile(target) as z:
            self.assertEqual(z.infolist()[0].filename, "rdpkg.json")
        self.assertEqual(validate_rdpkg_payload(target)["status"], "PAYLOAD_INTEGRITY_PASS")

    def test_stored_archive_keeps_stored(self):
        src = self.root / "a.rdpkg"
        make_rdpkg(src, method=ZIP_STORED)
        target, _ = fx.fix_file(src, GOOD)
        with ZipFile(target) as z:
            self.assertTrue(all(i.compress_type == ZIP_STORED for i in z.infolist()))

    def test_rdproject_with_volumes(self):
        vol = self.root / "v.rdpkg"
        make_rdpkg(vol)
        project = self.root / "Laguna-II.rdproject"
        with ZipFile(project, "w") as z:
            z.writestr(zinfo("rdproject.json"), json.dumps({"format": "x", "project": {"id": "p"}}))
            z.writestr(zinfo("volumes/NT8183A.rdpkg", ZIP_STORED), vol.read_bytes())
        self.assertEqual(fx.count_bad(project), 2)
        target, report = fx.fix_file(project, GOOD)
        self.assertEqual(report.count, 2)
        with ZipFile(target) as z:
            self.assertEqual(z.infolist()[0].filename, "rdproject.json")
            inner = self.root / "inner.rdpkg"
            inner.write_bytes(z.read("volumes/NT8183A.rdpkg"))
        self.assertEqual(fx.count_bad(inner), 0)
        self.assertEqual(validate_rdpkg_payload(inner)["status"], "PAYLOAD_INTEGRITY_PASS")

    def test_clean_volume_is_not_touched(self):
        src = self.root / "clean.rdpkg"
        make_rdpkg(src, logo=OTHER)
        self.assertEqual(fx.count_bad(src), 0)
        with self.assertRaises(fx.FixError):
            fx.fix_file(src, GOOD)

    def test_main_dry_run_then_apply(self):
        folder = self.root / "Download"
        folder.mkdir()
        make_rdpkg(folder / "NT8183A.rdpkg")
        make_rdpkg(folder / "clean.rdpkg", logo=OTHER)
        args = ["--directory", str(folder), "--logo", str(self.logo)]
        self.assertEqual(fx.main(args), 0)
        self.assertFalse((folder / "logo-fixed").exists())
        self.assertEqual(fx.main(args + ["--apply"]), 0)
        self.assertTrue((folder / "logo-fixed" / "NT8183A.rdpkg").exists())
        self.assertFalse((folder / "logo-fixed" / "clean.rdpkg").exists())
        # output folder is never scanned again
        self.assertEqual([p.name for p in fx.find_packages([folder])], ["NT8183A.rdpkg", "clean.rdpkg"])
        self.assertEqual(sorted(p.name for p in (folder / "logo-fixed").iterdir()), ["NT8183A.rdpkg"])

    def test_real_fingerprint_constants(self):
        self.patches[0].stop(); self.patches[1].stop()
        try:
            self.assertEqual(fx.BAD_SIZE, 84696)
            self.assertEqual(fx.BAD_SHA256, "26ba49e731c292c27b7d28a2e14e1478196f2906a6cd77d7d3a09032adc39f1a")
        finally:
            self.patches[0].start(); self.patches[1].start()

    def test_menu_item(self):
        repo = Path(__file__).resolve().parents[1]
        menu = (repo / "menu.sh").read_text(encoding="utf-8")
        self.assertIn("26 — Замінити зіпсоване лого в томах Laguna II", menu)
        self.assertIn("26)\n      fix_volume_logo", menu)
        script = (repo / "tools/termux/reno-fix-volume-logo.sh").read_text(encoding="utf-8")
        self.assertIn("fix_volume_logo.py", script)
        self.assertIn("docs/brand/renodocs-logo.gif", script)
        self.assertTrue((repo / "docs/brand/renodocs-logo.gif").is_file())


if __name__ == "__main__":
    unittest.main()
