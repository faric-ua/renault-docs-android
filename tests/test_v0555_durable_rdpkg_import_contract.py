from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def test_direct_rdpkg_import_is_not_activity_owned():
    project = read("ProjectActivity.kt")
    start = project.index("private fun handleRdpkgResult")
    end = project.index("private fun handleRdpkgExportResult", start)
    flow = project[start:end]
    assert "Thread {" not in flow
    assert "RdpkgImporter.install(" not in flow
    assert "RdpkgImportService.start(" in flow


def test_rdpkg_import_has_durable_run_store_and_service():
    service = read("RdpkgImportService.kt")
    store = read("RdpkgImportRunStore.kt")
    manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
    assert "RdpkgImporter.install(" in service
    assert "startForeground(" in service
    assert "RdpkgImportRunStore" in service
    assert "consumedFinishedAtMs" in store
    assert "fun consume(" in store
    assert 'android:name=".RdpkgImportService"' in manifest
    assert 'android:stopWithTask="false"' in manifest


def test_project_reattaches_and_consumes_terminal_import_once():
    project = read("ProjectActivity.kt")
    assert "refreshRdpkgImportRunState()" in project
    assert "rdpkgImportRunStore.consume(state.finishedAtMs)" in project
    assert "LocalDatasetDocumentsProvider.treeUriFor(packageId)" in project
    assert "importPreparedVolume(" in project
    assert "allowOverride = true" in project
