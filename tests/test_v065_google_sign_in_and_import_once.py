"""Renodocs 0.6.5 (owner 2026-10-18 16:33: «давай, починай»).

1. #133 step «вхід через Google»: Settings → «Акаунт Google» → «Увійти через Google» /
   «Вийти». Sign-in only shows who is signed in — it locks nothing, keeps no token, and
   «Вийти» asks first (owner rule 2026-10-12).
2. TODO №30 (owner 13:47–14:03): a volume that was not in the project came back as
   «Том оновлено» — written into the project twice. The project screen now claims the
   finished import first, then adds the volume.
"""

import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def read(name):
    return (SRC / f"{name}.kt").read_text(encoding="utf-8")


class GoogleSignInBuild(unittest.TestCase):
    def setUp(self):
        self.gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")

    def test_libraries(self):
        for dep in (
            'implementation("androidx.credentials:credentials:',
            'implementation("androidx.credentials:credentials-play-services-auth:',
            'implementation("com.google.android.libraries.identity.googleid:googleid:',
        ):
            self.assertIn(dep, self.gradle)

    def test_client_id_from_property_not_hardcoded_in_code(self):
        self.assertIn('providers.gradleProperty("renodocs.googleWebClientId")', self.gradle)
        self.assertIn('buildConfigField("String", "GOOGLE_WEB_CLIENT_ID"', self.gradle)
        props = (ROOT / "android/gradle.properties").read_text(encoding="utf-8")
        # Owner 17:59: the Web client of project RenoDocs (the Android client is separate and is
        # never put into the app — Google matches it by package + SHA-1).
        self.assertRegex(props, r"(?m)^renodocs\.googleWebClientId=\d+-[0-9a-z]+\.apps\.googleusercontent\.com$")
        # A real client ID looks like 1234-abc.apps.googleusercontent.com — never in the code.
        self.assertNotRegex(read("GoogleAccount"), r"\d+-[0-9a-z]+\.apps\.googleusercontent\.com")

    def test_package_stays_renodocs(self):
        # The Android OAuth client in Google Cloud is tied to this package + SHA-1.
        # Final package (owner 17:38): com.saney.renodocs (0.6.0–0.6.4) → com.farsan.renodocs.
        self.assertIn('applicationId = "com.farsan.renodocs"', self.gradle)
        self.assertNotIn('applicationId = "com.saney.renodocs"', self.gradle)
        setup = (ROOT / "docs/plans/2026-10-18_GOOGLE_SIGN_IN_SETUP.md").read_text(encoding="utf-8")
        self.assertIn("Package name: `com.farsan.renodocs`", setup)
        self.assertIn("41:02:35:0E:27:87:FD:53:8B:BF:58:A2:19:29:3A:13:22:35:E6:18", setup)

    def test_keep_rule_for_release(self):
        rules = (ROOT / "android/app/proguard-rules.pro").read_text(encoding="utf-8")
        self.assertIn("-keep class androidx.credentials.playservices.**", rules)


class GoogleSignInBehaviour(unittest.TestCase):
    def setUp(self):
        self.account = read("GoogleAccount")
        self.settings = read("SettingsActivity")

    def test_uses_credential_manager_sign_in_with_google(self):
        self.assertIn("GetSignInWithGoogleOption.Builder(GoogleAccountConfig.webClientId)", self.account)
        self.assertIn("getCredentialAsync(", self.account)
        self.assertIn("GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL", self.account)
        self.assertIn("clearCredentialStateAsync(", self.account)

    def test_keeps_only_email_and_name_no_token(self):
        store = self.account[self.account.index("class GoogleAccountStore"):self.account.index("object GoogleAccountFlow")]
        self.assertIn("KEY_EMAIL", store)
        self.assertIn("KEY_NAME", store)
        self.assertNotRegex(store, r"(?i)token")
        self.assertNotIn("idToken", self.account)

    def test_not_configured_says_so(self):
        self.assertIn("NOT_CONFIGURED_TEXT", self.account)
        self.assertIn("if (!GoogleAccountConfig.isConfigured)", self.account)
        self.assertIn("Застосунок працює й без входу.", self.account)

    def test_result_survives_rotation(self):
        self.assertIn("pendingMessage", self.account)
        self.assertIn("GoogleAccountFlow.listener = {", self.settings)
        self.assertIn("GoogleAccountFlow.listener = null", self.settings)

    def test_card_in_settings_and_sign_out_asks(self):
        self.assertIn('"Акаунт Google"', self.settings)
        self.assertIn('"Увійти через Google"', self.settings)
        self.assertIn("CONFIRM_SIGN_OUT -> buildSignOutDialog()", self.settings)
        sign_out = self.settings[self.settings.index("private fun buildSignOutDialog"):]
        sign_out = sign_out[:sign_out.index("\n    }\n") + 6]
        self.assertIn("ConfirmActions.confirm(", sign_out)
        self.assertIn("Томи, проєкти й налаштування лишаються.", sign_out)
        self.assertIn("GoogleAccountFlow.signOut(this)", sign_out)
        # The click on «Вийти» only opens the question.
        click = self.settings[self.settings.index("private fun onAccountButton"):]
        click = click[:click.index("\n    }\n")]
        self.assertIn("confirmDialogs.show(CONFIRM_SIGN_OUT)", click)
        self.assertNotIn("signOut(", click)

    def test_sign_in_locks_nothing(self):
        # No other screen asks for an account in 0.6.5.
        for name in ("MainActivity", "ProjectActivity", "DriveCatalogActivity", "VolumeDocumentationActivity"):
            self.assertNotIn("GoogleAccountStore", read(name), name)


class ImportAddedOnce(unittest.TestCase):
    def test_claim_before_add(self):
        project = read("ProjectActivity")
        refresh = project[project.index("private fun refreshRdpkgImportRunState"):]
        refresh = refresh[:refresh.index("\n    private fun ")]
        claim = refresh.index("if (!rdpkgImportRunStore.consume(state.finishedAtMs)) return")
        add = refresh.index("importPreparedVolume(")
        self.assertLess(claim, add)
        success = refresh[refresh.index(".onSuccess { imported ->"):refresh.index(".onFailure")]
        self.assertNotIn("consume(", success)
        self.assertIn("continueRdpkgQueue(succeeded = true", success)

    def test_failure_goes_on_with_queue(self):
        project = read("ProjectActivity")
        refresh = project[project.index("private fun refreshRdpkgImportRunState"):]
        failure = refresh[refresh.index(".onFailure { error ->"):]
        failure = failure[:failure.index("\n        }\n")]
        self.assertIn("continueRdpkgQueue(succeeded = false", failure)

    def test_log_lines(self):
        self.assertIn('const val IMPORT_LOG_TAG = "RenaultImport"', read("ProjectActivity"))
        service = read("RdpkgImportService")
        self.assertIn("SERVICE start while busy", service)
        self.assertIn("SERVICE start startId=", service)


class Release(unittest.TestCase):
    def test_version_and_whats_new(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('versionName = "0.6.5"', gradle)
        self.assertIn("versionCode = 175", gradle)
        news = (ROOT / "android/app/src/main/assets/whats_new.txt").read_text(encoding="utf-8")
        self.assertTrue(news.startswith("0.6.5\n"))
        self.assertIn("Акаунт Google", news.split("\n\n")[0])

    def test_guide_mentions_account(self):
        guide = (ROOT / "docs/ua/USER_GUIDE.md").read_text(encoding="utf-8")
        self.assertIn("Renodocs v0.6.5", guide)
        self.assertIn("«Акаунт Google»", guide)
        app = (ROOT / "android/app/src/main/assets/guide/index.html").read_text(encoding="utf-8")
        self.assertIn("Renodocs v0.6.5", app)
        self.assertTrue(re.search(r"Акаунт Google", app))


if __name__ == "__main__":
    unittest.main()
