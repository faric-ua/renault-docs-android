"""🔑 faric-keys-put.sh — копія ключа підпису в ~/Ключі (інспектор GitHub Storage, 06.11.2026).

Справжній keytool на одноразовому ключі, у тимчасовому HOME — за рядком таблиці саме цього репо.
Перевіряємо: копія + паролі.txt + відбиток.txt + контроль.sha256, оригінал не чіпається,
паролі не на екран, неправильний пароль — нічого не копіюється, інший ключ — поруч, не замість.
"""
import os
import re
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "tools" / "termux" / "faric-keys-put.sh"
ROW = re.search(r'^\s+"([^"]+)"\) echo "([^|]+)\|\$HOME/([^|]+)\|([^|]+)\|([^|]+)\|([^|]+)\|', SCRIPT.read_text(encoding="utf-8"), re.M)
NAME, REPO, KEY, PWFILE, KIND, ALIAS = ROW.groups()
PW = "dorama-alpha" if KIND.startswith("fixed:") else "pw123456"


@unittest.skipUnless(shutil.which("keytool"), "keytool недоступний")
class KeysPutTest(unittest.TestCase):
    def setUp(self):
        self.home = Path(tempfile.mkdtemp(prefix="keys-"))
        self.key = self.home / KEY
        self.key.parent.mkdir(parents=True)
        self.gen()
        self.write_pw(PW)

    def tearDown(self):
        shutil.rmtree(self.home, ignore_errors=True)

    def gen(self):
        subprocess.run(["keytool", "-genkeypair", "-keystore", str(self.key), "-storetype", "JKS", "-storepass", PW,
                        "-keypass", PW, "-alias", ALIAS, "-keyalg", "RSA", "-keysize", "2048", "-validity", "100",
                        "-dname", "CN=Test"], check=True, capture_output=True)

    def write_pw(self, keypass):
        if KIND == "props":
            f = Path(PWFILE.replace("$HOME", str(self.home)))
            f.write_text(f"storeFile={self.key}\nstorePassword={PW}\nkeyAlias={ALIAS}\nkeyPassword={keypass}\n")
        elif KIND.startswith("env:"):
            p = KIND[4:]
            f = Path(PWFILE.replace("$HOME", str(self.home)))
            f.write_text(f"export {p}_STORE_PASSWORD='{PW}'\nexport {p}_KEY_ALIAS='{ALIAS}'\nexport {p}_KEY_PASSWORD='{keypass}'\n")

    def run_put(self):
        env = dict(os.environ, HOME=str(self.home))
        return subprocess.run(["bash", str(SCRIPT), NAME], capture_output=True, text=True, env=env, timeout=120)

    def test_copies_key_with_passwords_and_checksums(self):
        before = self.key.read_bytes()
        r = self.run_put()
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        d = self.home / "Ключі" / NAME
        self.assertEqual((d / self.key.name).read_bytes(), before)
        self.assertEqual(self.key.read_bytes(), before)
        pw = (d / "паролі.txt").read_text()
        self.assertIn(f"Застосунок: {NAME} (faric-ua/{REPO})", pw)
        self.assertIn(f"Пароль файлу (storePassword): {PW}", pw)
        self.assertIn(f"Назва ключа (keyAlias): {ALIAS}", pw)
        self.assertRegex((d / "відбиток.txt").read_text(), r"SHA-256 сертифіката: [0-9a-f]{64}")
        self.assertEqual(subprocess.run(["sha256sum", "-c", "контроль.sha256"], cwd=d, capture_output=True).returncode, 0)
        if not KIND.startswith("fixed:"):
            self.assertNotIn(PW, r.stdout)
        self.assertNotIn("python3" + "0", r.stdout)
        self.assertEqual(oct(d.stat().st_mode & 0o777), "0o700")

    @unittest.skipIf(KIND.startswith("fixed:"), "пароль вбудований")
    def test_wrong_password_copies_nothing(self):
        self.write_pw("wrong")
        r = self.run_put()
        self.assertNotEqual(r.returncode, 0)
        self.assertIn("НЕ скопійовано", r.stdout)
        self.assertFalse((self.home / "Ключі" / NAME).exists())

    def test_other_key_is_put_alongside(self):
        self.assertEqual(self.run_put().returncode, 0)
        self.assertIn("уже лежить", self.run_put().stdout)
        self.key.unlink()
        self.gen()
        r = self.run_put()
        self.assertEqual(r.returncode, 0, r.stdout)
        self.assertIn("ІНШИЙ ключ", r.stdout)
        self.assertEqual(len([p for p in (self.home / "Ключі").iterdir() if p.name.startswith(f"{NAME} (новий")]), 1)

    def test_missing_key_is_reported(self):
        self.key.unlink()
        r = self.run_put()
        self.assertNotEqual(r.returncode, 0)
        self.assertIn("Ключа нема", r.stdout)


if __name__ == "__main__":
    unittest.main()
