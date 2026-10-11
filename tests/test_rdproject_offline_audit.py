"""Stage J: offline nested .rdproject integrity checks using synthetic packages."""
import io
import json
import tempfile
from pathlib import Path
from zipfile import ZipFile
import unittest

from tools.audit_rdproject import audit_rdproject


def package(nt, project_id="megane-ii"):
    source=f"Megane-II_{nt}/INDEX.HTM"
    dataset={"project_id":project_id,
             "volumes":[{"id":nt,"document_code":nt,"entrypoint":source}]}
    payload={"renault-dataset.json":json.dumps(dataset).encode(),
             source:b"<html></html>"}
    manifest={"schema_version":1,"format":"renault-volume-package-v1",
              "project_id":project_id,"package_id":nt,
              "volume":dataset["volumes"][0],
              "payload_file_count":len(payload),
              "payload_bytes":sum(len(x) for x in payload.values())}
    out=io.BytesIO()
    with ZipFile(out,"w") as z:
        z.writestr("rdpkg.json",json.dumps(manifest))
        for name,data in payload.items(): z.writestr(name,data)
    return out.getvalue()


def project(path, nts=("NT8340A","NT8342A"), *, invalid=""):
    volumes=[]
    with ZipFile(path,"w") as z:
        for i,nt in enumerate(nts):
            name=f"volumes/Megane-{nt}.rdpkg"
            volumes.append({"document_code":nt,"file":name})
            data=package("NT9999" if invalid=="wrong_nt" and i==0 else nt)
            if invalid=="corrupt" and i==0: data=data[:-3]
            z.writestr(name,data)
        if invalid=="extra": z.writestr("unexpected.txt","extra")
        z.writestr("rdproject.json",json.dumps({
            "format":"renault-docs-project","schema_version":1,
            "project":{"id":"megane-ii","title":"Megane II"},
            "volumes":volumes}))


class RdprojectAuditTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory()
        self.path=Path(self.temp.name)/"Megane-II.rdproject"

    def tearDown(self):
        self.temp.cleanup()

    def test_valid_two_distinct_nt_and_does_not_modify_archives(self):
        project(self.path)
        before=self.path.read_bytes()
        result=audit_rdproject(self.path,expected_volumes=2)
        self.assertEqual("PASS",result["status"])
        self.assertEqual(2,result["volume_count"])
        self.assertEqual(["NT8340A","NT8342A"],
            [record["document_code"] for record in result["volumes"]])
        self.assertTrue(all(record["crc"]=="PASS" for record in result["volumes"]))
        self.assertEqual(before,self.path.read_bytes())
        self.assertEqual([self.path.name],[x.name for x in self.path.parent.iterdir()])

    def test_mismatched_nt_is_rejected(self):
        project(self.path,invalid="wrong_nt")
        with self.assertRaisesRegex(ValueError,"identity"):
            audit_rdproject(self.path)

    def test_undeclared_outer_file_is_rejected(self):
        project(self.path,invalid="extra")
        with self.assertRaisesRegex(ValueError,"file set"):
            audit_rdproject(self.path)

    def test_bad_inner_zip_is_rejected(self):
        project(self.path,invalid="corrupt")
        with self.assertRaises(Exception):
            audit_rdproject(self.path)

    def test_expected_count_mismatch_is_not_reported_pass(self):
        project(self.path)
        with self.assertRaisesRegex(ValueError,"volume count"):
            audit_rdproject(self.path,expected_volumes=21)


if __name__=="__main__":
    unittest.main()
