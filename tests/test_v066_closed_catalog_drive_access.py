"""Renodocs 0.6.6 — #133 closed catalogue for friends, option (а) (owner 2026-10-18 18:31:
«давай А попробуємо»).

A friend = a Google account the owner gave access to his catalogue folder on Drive.
The app reads the catalogue and volumes through the Drive API with the signed-in account's
permission `drive.readonly`. While the folder is still open «to anyone with the link», the old
public way is the fallback — nothing breaks before the owner closes the folder (only with his
«так»). Nothing is built into the APK: no friends list, no keys; the token lives in memory only.
"""

import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def read(name):
    return (SRC / f"{name}.kt").read_text(encoding="utf-8")


class DriveAccessToken(unittest.TestCase):
    def setUp(self):
        self.access = read("DriveAccess")

    def test_scope_is_drive_readonly(self):
        self.assertIn('const val SCOPE = "https://www.googleapis.com/auth/drive.readonly"', self.access)
        self.assertIn("Identity.getAuthorizationClient(", self.access)
        self.assertIn("AuthorizationRequest.builder()", self.access)

    def test_token_in_memory_only(self):
        # Never saved: no SharedPreferences, no files.
        self.assertNotIn("SharedPreferences", self.access)
        self.assertNotIn("getSharedPreferences", self.access)
        self.assertNotIn("FileOutputStream", self.access)
        self.assertIn("@Volatile\n    private var token: String? = null", self.access)

    def test_silent_never_shows_a_window(self):
        silent = self.access[self.access.index("fun tokenSilently("):self.access.index("fun ask(")]
        self.assertIn("if (result.hasResolution()) null", silent)
        self.assertIn("if (!isSignedIn(context)) return null", silent)

    def test_sign_out_forgets_token(self):
        account = read("GoogleAccount")
        sign_out = account[account.index("fun signOut("):]
        self.assertIn("DriveAccess.forget()", sign_out)

    def test_no_friend_list_or_secret_in_code(self):
        for name in ("DriveAccess", "DriveCatalogClient", "DriveCatalogActivity"):
            text = read(name)
            self.assertNotRegex(text, r"[a-z0-9._%+-]+@gmail\.com", name)
            self.assertNotRegex(text, r"(?i)client_secret|api_key|AIza[0-9A-Za-z_-]{20,}", name)


class DriveCatalogClientAccess(unittest.TestCase):
    def setUp(self):
        self.client = read("DriveCatalogClient")

    def test_account_first_then_public_fallback(self):
        open_file = self.client[self.client.index("private fun openFile("):self.client.index("fun driveApiUrl(")]
        account = open_file.index("driveApiUrl(")
        public = open_file.index("ExternalLinks.driveDownloadUrl(")
        self.assertLess(account, public)
        self.assertIn("return connection to DriveVia.ACCOUNT", open_file)
        self.assertIn("return connection to DriveVia.PUBLIC", open_file)
        # A Google sign-in page instead of the file = closed, never parsed as a catalogue.
        self.assertIn('"text/html"', open_file)

    def test_drive_api_url(self):
        self.assertIn('"https://www.googleapis.com/drive/v3/files/"', self.client)
        self.assertIn('"?alt=media&supportsAllDrives=true"', self.client)
        self.assertIn('"Bearer $bearer"', self.client)

    def test_clear_messages(self):
        open_file = self.client[self.client.index("private fun openFile("):self.client.index("fun driveApiUrl(")]
        self.assertIn("401 ->", open_file)
        self.assertIn("DriveAccess.forget()", open_file)
        self.assertIn("404 ->", open_file)
        self.assertIn("DriveAccessTexts.noAccess(", open_file)
        self.assertIn("DriveAccessTexts.CLOSED_SIGN_IN", open_file)
        texts = read("DriveAccess")
        self.assertIn("Каталог закритий. Увійди через Google", texts)
        self.assertIn("Попроси автора відкрити каталог для цієї пошти.", texts)
        # A server/network error is not reported as «closed».
        self.assertIn("CLOSED_CODES = setOf(401, 403, 404)", self.client)
        self.assertIn('"HTTP $code при доступі до каталогу."', self.client)

    def test_all_callers_pass_context(self):
        self.assertIn("fun loadCatalog(\n        context: Context,", self.client)
        self.assertIn("fun downloadPackage(\n        context: Context,", self.client)
        self.assertNotRegex(read("ProjectExportWizard"), r"DriveCatalogClient\.loadCatalog\(\)")
        self.assertIn("DriveCatalogClient.loadCatalog(activity.applicationContext)", read("ProjectExportWizard"))
        service = read("CatalogImportService")
        self.assertRegex(service, r"\.downloadPackage\(\s*context =\s*applicationContext,")

    def test_download_still_checks_size_and_sha(self):
        self.assertIn("Контрольна сума .rdpkg не збігається з каталогом", self.client)
        self.assertIn("Розмір завантаженого .rdpkg не збігається з каталогом", self.client)


class CatalogScreen(unittest.TestCase):
    def setUp(self):
        self.screen = read("DriveCatalogActivity")

    def test_asks_google_once_when_signed_in(self):
        load = self.screen[self.screen.index("private fun loadCatalog()"):self.screen.index("override fun onActivityResult(")]
        self.assertIn("!driveAsked &&", load)
        self.assertIn("DriveAccess.isSignedIn(this)", load)
        self.assertIn("startIntentSenderForResult(", load)
        self.assertIn("REQUEST_DRIVE_ACCESS", load)

    def test_window_answer_and_rotation(self):
        self.assertIn("DriveAccess.tokenFromResult(", self.screen)
        self.assertIn("STATE_DRIVE_ASKED", self.screen)
        self.assertIn("STATE_DRIVE_WINDOW", self.screen)
        self.assertIn("} else if (driveWindowOpen) {", self.screen)

    def test_status_shows_how_opened(self):
        self.assertIn('DriveVia.ACCOUNT -> " · через акаунт Google"', self.screen)
        self.assertIn('DriveVia.PUBLIC -> " · відкритий доступ"', self.screen)
        self.assertIn("if (error is DriveAccessException)", self.screen)


class Release(unittest.TestCase):
    def test_version(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('versionName = "0.6.6"', gradle)
        self.assertIn("versionCode = 176", gradle)
        self.assertIn('implementation("com.google.android.gms:play-services-auth:', gradle)
        # Package stays final (owner 2026-10-18 17:38) — the OAuth clients are tied to it.
        self.assertIn('applicationId = "com.farsan.renodocs"', gradle)

    def test_whats_new_and_guide(self):
        news = (ROOT / "android/app/src/main/assets/whats_new.txt").read_text(encoding="utf-8")
        self.assertTrue(news.startswith("0.6.6\n"))
        self.assertIn("через твій акаунт Google", news.split("\n\n")[0])
        guide = (ROOT / "docs/ua/USER_GUIDE.md").read_text(encoding="utf-8")
        self.assertIn("Renodocs v0.6.6", guide)
        self.assertIn("Через акаунт Google", guide)
        app = (ROOT / "android/app/src/main/assets/guide/index.html").read_text(encoding="utf-8")
        self.assertIn("Renodocs v0.6.6", app)
        self.assertTrue(re.search(r"Через акаунт Google", app))

    def test_setup_doc_has_drive_steps(self):
        setup = (ROOT / "docs/plans/2026-10-18_GOOGLE_SIGN_IN_SETUP.md").read_text(encoding="utf-8")
        self.assertIn("Google Drive API", setup)
        self.assertIn("drive.readonly", setup)


if __name__ == "__main__":
    unittest.main()
