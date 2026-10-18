"""Phone Diagnostics 35 — «Прибрати сміття в проєктах» (faric 2026-10-18).

Функцію виконуємо по-справжньому на штучному «телефоні» в тимчасовій папці:
видаляє лише вибране після «так» і ніколи не чіпає захищене.
"""
import os
import re
import shutil
import subprocess
import tempfile
import time
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MENU = ROOT / "tools" / "termux" / "phone-diagnostics-menu.sh"


def junk_cleanup_source():
    text = MENU.read_text(encoding="utf-8")
    m = re.search(r"# junk_cleanup start\n(.*?)# junk_cleanup end\n", text, re.S)
    assert m, "markers junk_cleanup start/end not found"
    return m.group(1)


def write(path: Path, data: bytes = b"x" * 4096, age_days: float = 0):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)
    if age_days:
        t = time.time() - age_days * 86400
        os.utime(path, (t, t))
    return path


@unittest.skipUnless(shutil.which("bash") and shutil.which("git"), "bash and git required")
class JunkCleanupTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp(prefix="pd35-"))
        self.home = self.tmp / "home"
        self.docs = self.tmp / "sd" / "Documents"
        self.prefix = self.tmp / "usr"
        (self.prefix / "tmp").mkdir(parents=True)

        # Репо з результатами збірки і одним build/, записаним у git (не чіпати).
        repo = self.home / "faric-track-catcher"
        write(repo / "android" / "settings.gradle.kts", b"")
        write(repo / "android" / "app" / "build.gradle.kts", b"")
        write(repo / "android" / "app" / "src" / "Main.kt", b"code")
        write(repo / "android" / "app" / "build" / "outputs" / "app.apk")
        write(repo / "android" / ".gradle" / "8.13" / "fileHashes.bin")
        write(repo / "tools" / "build.gradle", b"")
        write(repo / "tools" / "build" / "tracked.txt", b"keep me")
        subprocess.run(["git", "init", "-q", str(repo)], check=True)
        subprocess.run(["git", "-C", str(repo), "add", "android/app/src", "tools"], check=True)
        self.repo = repo

        # APK: 3 версії Track Catcher (лишаються 2 найновіші), Renault — APK і том.
        write(self.docs / "Track Catcher" / "apk" / "v0.1.0" / "tc-0.1.0.apk", age_days=9)
        write(self.docs / "Track Catcher" / "apk" / "v0.2.0" / "tc-0.2.0.apk", age_days=5)
        write(self.docs / "Track Catcher" / "apk" / "v0.3.0" / "tc-0.3.0.apk", age_days=1)
        write(self.docs / "Track Catcher" / "recordings" / "song.wav")
        for i, v in enumerate(["0.6.1", "0.6.2", "0.6.3"]):
            write(self.docs / "Renault" / "packages" / f"Renault-Docs-v{v}-build" / f"Renault-Docs-v{v}-debug.apk", age_days=9 - i)
        write(self.docs / "Renault" / "packages" / "rdpkg" / "megane.rdpkg", age_days=30)
        write(self.docs / "Renault" / "packages" / "legacy" / "old-volume.apk", age_days=30)
        write(self.docs / "PhoneDiagnostics" / "apk" / "aolithium_v1.3" / "base.apk", age_days=30)
        write(self.docs / "PhoneDiagnostics" / "apk" / "aolithium_v1.3.apks", age_days=40)
        write(self.docs / "PhoneDiagnostics" / "apk" / "other_v1.apks", age_days=50)
        write(self.tmp / "sd" / "Download" / "SAI.apk", age_days=30)

        # Termux: скачане, тимчасове, ключі, кеш Gradle.
        write(self.home / ".cache" / "track-catcher" / "phone-build-download" / "a.apk")
        write(self.home / ".cache" / "renault-docs" / "state.env", b"state")
        write(self.home / ".android" / "track-catcher.jks", b"key")
        write(self.home / "gradle" / "gradle-8.13" / "bin" / "gradle", b"")
        write(self.home / ".gradle" / "caches" / "8.13" / "x.bin")
        write(self.home / ".gradle" / "caches" / "8.7" / "old.bin")
        write(self.home / ".gradle" / "caches" / "modules-2" / "dep.jar")
        write(self.home / ".gradle" / "daemon" / "8.7" / "daemon.out.log", age_days=3)
        write(self.prefix / "tmp" / "old-stuff" / "f", age_days=3)
        old = time.time() - 3 * 86400
        os.utime(self.prefix / "tmp" / "old-stuff", (old, old))
        write(self.prefix / "tmp" / "fresh" / "f")

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def run_cleanup(self, answers: str):
        script = (
            "set -u\n"
            "new_log() { CURRENT_LOG=\"$LOGF\"; : > \"$CURRENT_LOG\"; }\n"
            "pause_menu() { :; }\n"
            "clear() { :; }\n"
            "getprop() { echo TestPhone; }\n"
            + junk_cleanup_source()
            + "junk_cleanup\n"
        )
        env = dict(os.environ, JC_HOME=str(self.home), JC_DOCS=str(self.docs),
                   JC_PREFIX=str(self.prefix), LOGF=str(self.tmp / "log.txt"))
        res = subprocess.run(["bash", "-c", script], input=answers, text=True,
                             capture_output=True, env=env, timeout=60)
        self.assertEqual(res.returncode, 0, res.stderr)
        return res.stdout + (self.tmp / "log.txt").read_text(encoding="utf-8")

    def assert_protected_intact(self):
        for p in [
            self.repo / "android" / "app" / "src" / "Main.kt",
            self.repo / "tools" / "build" / "tracked.txt",
            self.repo / ".git",
            self.docs / "Track Catcher" / "apk" / "v0.3.0" / "tc-0.3.0.apk",
            self.docs / "Track Catcher" / "apk" / "v0.2.0" / "tc-0.2.0.apk",
            self.docs / "Track Catcher" / "recordings" / "song.wav",
            self.docs / "Renault" / "packages" / "Renault-Docs-v0.6.3-build" / "Renault-Docs-v0.6.3-debug.apk",
            self.docs / "Renault" / "packages" / "Renault-Docs-v0.6.2-build" / "Renault-Docs-v0.6.2-debug.apk",
            self.docs / "Renault" / "packages" / "rdpkg" / "megane.rdpkg",
            self.docs / "Renault" / "packages" / "legacy" / "old-volume.apk",
            self.docs / "PhoneDiagnostics" / "apk" / "aolithium_v1.3" / "base.apk",
            self.docs / "PhoneDiagnostics" / "apk" / "aolithium_v1.3.apks",
            self.tmp / "sd" / "Download" / "SAI.apk",
            self.home / ".cache" / "renault-docs" / "state.env",
            self.home / ".android" / "track-catcher.jks",
            self.home / "gradle" / "gradle-8.13" / "bin" / "gradle",
            self.home / ".gradle" / "caches" / "8.13" / "x.bin",
            self.home / ".gradle" / "caches" / "modules-2" / "dep.jar",
            self.prefix / "tmp" / "fresh" / "f",
        ]:
            self.assertTrue(p.exists(), f"protected path was removed: {p}")

    def test_cancel_by_default_removes_nothing(self):
        out = self.run_cleanup("\n")
        self.assertIn("Нічого не видалено", out)
        self.assertTrue((self.repo / "android" / "app" / "build").exists())
        self.assertTrue((self.docs / "Track Catcher" / "apk" / "v0.1.0" / "tc-0.1.0.apk").exists())
        self.assert_protected_intact()

    def test_without_tak_removes_nothing(self):
        out = self.run_cleanup("все\nні\n")
        self.assertIn("Скасовано", out)
        self.assertTrue((self.home / ".gradle" / "caches" / "8.7").exists())
        self.assert_protected_intact()

    def test_all_groups_after_tak(self):
        out = self.run_cleanup("все\nтак\n")
        self.assertIn("ГОТОВО", out)
        self.assertIn("APK у Download: 1 шт.", out)
        self.assertFalse((self.repo / "android" / "app" / "build").exists())
        self.assertFalse((self.repo / "android" / ".gradle").exists())
        self.assertFalse((self.docs / "Track Catcher" / "apk" / "v0.1.0").exists())
        self.assertFalse((self.docs / "Renault" / "packages" / "Renault-Docs-v0.6.1-build").exists())
        self.assertFalse((self.home / ".cache" / "track-catcher").exists())
        self.assertFalse((self.home / ".gradle" / "caches" / "8.7").exists())
        self.assertFalse((self.home / ".gradle" / "daemon" / "8.7").exists())
        self.assertFalse((self.prefix / "tmp" / "old-stuff").exists())
        self.assert_protected_intact()
        self.assertNotIn("НЕ ВИЙШЛО", out)

    def test_only_chosen_group(self):
        self.run_cleanup("2\nтак\n")
        self.assertFalse((self.docs / "Track Catcher" / "apk" / "v0.1.0").exists())
        self.assertTrue((self.repo / "android" / "app" / "build").exists())
        self.assertTrue((self.home / ".gradle" / "caches" / "8.7").exists())
        self.assert_protected_intact()


class JunkCleanupContractTest(unittest.TestCase):
    def test_menu_item_35_wired(self):
        text = MENU.read_text(encoding="utf-8")
        self.assertIn('echo "35 — Прибрати сміття в проєктах', text)
        self.assertIn("    35) junk_cleanup ;;", text)

    def test_safety_rules_in_code(self):
        src = junk_cleanup_source()
        self.assertIn('[ "$answer" != "так" ]', src)
        self.assertIn("jc_safe_path", src)
        self.assertIn("jc_build_running", src)
        self.assertIn('"$JC_HOME"/*/ "$JC_HOME"/*/*/ "$JC_DOCS"/*/', src)
        # Нічого через ADB: лише цей телефон.
        self.assertNotIn("adb ", src)
        # Без git clean / reset — історію не чіпаємо.
        self.assertNotIn("git clean", src)
        self.assertNotIn("reset --hard", src)


if __name__ == "__main__":
    unittest.main()
