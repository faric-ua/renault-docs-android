from pathlib import Path
import subprocess
import unittest


class TermuxMenuContractTests(unittest.TestCase):
    def test_renault_menu_has_expected_primary_actions(self):
        repo = Path(__file__).resolve().parents[1]
        text = (repo / "menu.sh").read_text(encoding="utf-8")

        self.assertIn('1 — Відкрити браузерну документацію', text)
        self.assertIn('bash "$REPO/browser.sh"', text)
        self.assertIn('bash "$REPO/tools/termux/reno-status.sh"', text)
        self.assertIn('bash "$REPO/tools/termux/reno-stop.sh"', text)
        self.assertIn('7 — Build + Download APK', text)
        self.assertIn('8 — Download APK для поточного commit', text)
        self.assertIn('9 — Оновити Fast/Modern package', text)
        self.assertIn('python tools/package_dataset.py', text)
        self.assertIn('bash "$REPO/tools/termux/reno-build-apk.sh"', text)
        self.assertIn('bash "$REPO/tools/termux/reno-download-apk.sh"', text)
        self.assertIn('13 — Відкрити папку останнього APK', text)
        self.assertIn('bash "$REPO/tools/termux/reno-open-latest-apk.sh"', text)
        self.assertIn('14 — Швидкий конвертер (Termux/direct filesystem)', text)
        self.assertIn('bash "$REPO/tools/termux/reno-fast-convert.sh"', text)
        self.assertIn('19 — Статус проєкту / build', text)
        self.assertIn('bash "$REPO/tools/termux/reno-project-status.sh"', text)
        self.assertIn('20 — Відновити accepted signer + build', text)
        self.assertIn('21 — Аудит legacy *_android (read-only)', text)
        self.assertIn('22 — Перевірити посилання dataset (read-only)', text)
        self.assertIn('bash "$REPO/tools/termux/reno-check-dataset-links.sh"', text)
        self.assertIn('23 — Legacy quarantine *_android (move/restore, без видалення)', text)
        self.assertIn('bash "$REPO/tools/termux/reno-quarantine-legacy-android.sh"', text)
        self.assertNotIn('build"\\n  echo "21 —', text)
        self.assertIn('bash "$REPO/tools/termux/reno-restore-accepted-signer.sh"', text)
        self.assertNotIn('Phone Diagnostics Menu', text)

    def test_renault_operational_paths_use_private_termux_checkout(self):
        repo = Path(__file__).resolve().parents[1]
        forbidden = "/storage/emulated/0/Documents/Renault/application"

        files = [
            repo / "menu.sh",
            repo / "browser.sh",
            repo / "config" / "current-device.json",
            repo / "tools" / "install_termux_aliases.sh",
            repo / "tools" / "install_termux_widget.sh",
            repo / "tools" / "termux" / "reno-docs.sh",
            repo / "tools" / "termux" / "reno-build-apk.sh",
            repo / "tools" / "termux" / "reno-download-apk.sh",
            repo / "tools" / "termux" / "reno-fast-convert.sh",
        ]

        for path in files:
            with self.subTest(path=path):
                self.assertNotIn(
                    forbidden,
                    path.read_text(encoding="utf-8"),
                )

        aliases = (repo / "tools" / "install_termux_aliases.sh").read_text(
            encoding="utf-8"
        )
        widget = (repo / "tools" / "install_termux_widget.sh").read_text(
            encoding="utf-8"
        )
        config = (repo / "config" / "current-device.json").read_text(
            encoding="utf-8"
        )

        self.assertIn('REPO="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"', aliases)
        self.assertIn('RENAULT_REPO="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"', widget)
        self.assertIn(
            '"repository_root": "/data/data/com.termux/files/home/renault-docs-android"',
            config,
        )
        self.assertIn(
            '"source_root": "/storage/emulated/0/Documents/Renault/laguna 2 2001-2006"',
            config,
        )
        self.assertIn(
            '"build_root": "/storage/emulated/0/Documents/Renault/legacy-quarantine/laguna 2 2001-2006_android"',
            config,
        )

    def test_widget_has_two_project_buttons_and_does_not_manage_phone_diagnostics(self):
        repo = Path(__file__).resolve().parents[1]
        text = (repo / "tools" / "install_termux_widget.sh").read_text(encoding="utf-8")

        self.assertIn('cat > "$SHORTCUT_DIR/Renault"', text)
        self.assertIn('cat > "$SHORTCUT_DIR/YTM Importer"', text)
        self.assertIn('exec bash "$RENAULT_REPO/menu.sh"', text)
        self.assertIn('exec bash "$YTM_REPO/tools/termux/ytm-menu.sh"', text)
        self.assertNotIn('cat > "$SHORTCUT_DIR/Phone Diagnostics"', text)
        self.assertNotIn('phone-diagnostics-menu.sh"', text)
        self.assertIn('Phone Diagnostics має власний окремий installer', text)

    def test_phone_diagnostics_menu_is_standalone_and_logged(self):
        repo = Path(__file__).resolve().parents[1]
        text = (
            repo / "tools" / "termux" / "phone-diagnostics-menu.sh"
        ).read_text(encoding="utf-8")

        self.assertIn('Phone Diagnostics Menu', text)
        self.assertIn('Інструкція: Wi-Fi debugging крок за кроком', text)
        self.assertIn('Pair device за 6-значним кодом', text)
        self.assertIn('ADB connect до звичайного Wi-Fi debugging порту', text)
        self.assertIn('CPU snapshot → файл', text)
        self.assertIn('CPU/Media monitor 30 секунд → файл', text)
        self.assertIn('TOP snapshot → файл', text)
        self.assertIn('Media / Photo / Gallery процеси → файл', text)
        self.assertIn('Audio diagnostics → файл', text)
        self.assertIn('RAM / swap → файл', text)
        self.assertIn('Повний diagnostic bundle → файл', text)
        self.assertIn('force-stop Google MediaProvider', text)
        self.assertIn('force-stop Samsung MediaProvider', text)
        self.assertIn('Відкрити папку логів у файловому менеджері', text)
        self.assertIn('android.intent.action.VIEW', text)
        self.assertIn('android.intent.action.OPEN_DOCUMENT_TREE', text)
        self.assertIn('primary%3ADocuments%2FPhoneDiagnostics%2Flogs', text)
        self.assertIn('PRIMARY_LOG_DIR="$BASE_DIR/logs"', text)
        self.assertIn('STATE_DIR="$BASE_DIR/state"', text)
        self.assertIn('sequence', text)
        self.assertIn('item-', text)
        self.assertIn('adb pair "$ip:$port"', text)
        self.assertIn('adb connect "$target"', text)
        self.assertIn('dumpsys cpuinfo', text)
        self.assertIn('shell top -b -n 1', text)
        self.assertIn('com.google.android.providers.media.module', text)
        self.assertIn('com.samsung.android.providers.media', text)

    def test_phone_diagnostics_cleanup_report_is_read_only_and_self_update_exists(self):
        repo = Path(__file__).resolve().parents[1]
        text = (
            repo / "tools" / "termux" / "phone-diagnostics-menu.sh"
        ).read_text(encoding="utf-8")

        self.assertIn('17 — Звіт для чистки → файл (нічого не змінює)', text)
        self.assertIn('18 — Оновити це меню з GitHub', text)
        self.assertIn('17) cleanup_report ;;', text)
        self.assertIn('18) update_self ;;', text)
        self.assertRegex(text, r'PD_VERSION="\d{4}-\d{2}-\d{2}\.\d+"')

        start = text.index('cleanup_report() {')
        end = text.index('update_self() {')
        report = text[start:end]
        self.assertIn('am get-standby-bucket', report)
        self.assertIn('dumpsys diskstats', report)
        self.assertIn('Total PSS by process', report)
        for forbidden in (
            'settings put', 'pm disable', 'pm uninstall', 'pm clear',
            'trim-caches', 'force-stop', 'appops set', 'rm -rf', 'reboot',
        ):
            self.assertNotIn(forbidden, report)

        update = text[end:text.index('set_animations() {')]
        self.assertIn('gh api "repos/$PD_REPO/contents/$PD_SOURCE_PATH"', update)
        self.assertIn('grep -q "Phone Diagnostics Menu"', update)
        self.assertIn('exec bash "$menu_path"', update)

    def test_phone_diagnostics_animations_are_reversible_and_verified(self):
        repo = Path(__file__).resolve().parents[1]
        text = (
            repo / "tools" / "termux" / "phone-diagnostics-menu.sh"
        ).read_text(encoding="utf-8")

        self.assertIn('19) set_animations "0.5"', text)
        self.assertIn('20) set_animations "1"', text)
        fn = text[text.index('set_animations() {'):text.index('package_label() {')]
        for key in ('window_animation_scale', 'transition_animation_scale', 'animator_duration_scale'):
            self.assertIn(key, fn)
        self.assertIn('settings get global', fn)
        self.assertIn('Налагодження USB (налаштування безпеки)', fn)
        for forbidden in ('pm disable', 'pm uninstall', 'pm clear', 'trim-caches', 'reboot'):
            self.assertNotIn(forbidden, fn)

    def test_phone_diagnostics_bloat_is_disabled_not_removed_and_reversible(self):
        repo = Path(__file__).resolve().parents[1]
        text = (
            repo / "tools" / "termux" / "phone-diagnostics-menu.sh"
        ).read_text(encoding="utf-8")

        self.assertIn('21) set_bloat_enabled "disable"', text)
        self.assertIn('22) set_bloat_enabled "enable"', text)
        line = next(l for l in text.splitlines() if l.startswith('BLOAT_PACKAGES='))
        self.assertIn('com.miui.msa.global', line)
        self.assertIn('com.mi.globalminusscreen', line)
        self.assertIn('com.google.android.apps.magazines', line)
        # Пошук Google, Play, YouTube, Chrome, Карти — не чіпати.
        for keep in ('googlequicksearchbox', 'com.android.vending', 'com.google.android.youtube',
                     'com.android.chrome', 'com.google.android.apps.maps', 'com.google.android.gms'):
            self.assertNotIn(keep, line)
        # Бабусин телефон: банк, Дія, Київстар, газ, месенджери — ніколи.
        for keep in ('ua.privatbank', 'ua.gov.diia', 'kyivstar', 'vinnytsia.gas',
                     'com.facebook.katana', 'com.facebook.orca', 'whatsapp', 'viber', 'telegram'):
            self.assertNotIn(keep, line)
        fn = text[text.index('set_bloat_enabled() {'):text.index('set_apps_sleep() {')]
        self.assertIn('pm disable-user --user 0', fn)
        self.assertIn('pm enable --user 0', fn)
        self.assertIn('Продовжити? Напиши так / ні', fn)
        self.assertIn('pm list packages -d', fn)
        for forbidden in ('pm uninstall', 'pm clear', 'trim-caches', 'rm -', 'reboot'):
            self.assertNotIn(forbidden, fn)

    def test_phone_diagnostics_sleep_apps_is_reversible_and_spares_grandma_apps(self):
        repo = Path(__file__).resolve().parents[1]
        text = (
            repo / "tools" / "termux" / "phone-diagnostics-menu.sh"
        ).read_text(encoding="utf-8")

        self.assertIn('23) set_apps_sleep "sleep"', text)
        self.assertIn('24) set_apps_sleep "wake"', text)
        line = next(l for l in text.splitlines() if l.startswith('SLEEP_PACKAGES='))
        for keep in ('ua.privatbank', 'ua.gov.diia', 'kyivstar', 'vinnytsia.gas',
                     'com.facebook.katana', 'com.facebook.orca', 'whatsapp', 'viber',
                     'telegram', 'com.google.android.verifier', 'tachyon'):
            self.assertNotIn(keep, line)
        fn = text[text.index('set_apps_sleep() {'):text.index('open_with_intent() {')]
        self.assertIn('RUN_ANY_IN_BACKGROUND ignore', fn)
        self.assertIn('RUN_ANY_IN_BACKGROUND allow', fn)
        self.assertIn('set-standby-bucket "$pkg" restricted', fn)
        self.assertIn('set-standby-bucket "$pkg" active', fn)
        self.assertIn('Продовжити? Напиши так / ні (можна y / n)', fn)
        for forbidden in ('pm uninstall', 'pm clear', 'pm disable', 'trim-caches', 'force-stop', 'rm -', 'reboot'):
            self.assertNotIn(forbidden, fn)

    def test_phone_diagnostics_samsung_lists_spare_recording_apps(self):
        # 2026-10-20 (чат Track Catcher, T85): M30s пише музику з ривками — свої списки для Samsung.
        repo = Path(__file__).resolve().parents[1]
        text = (repo / "tools" / "termux" / "phone-diagnostics-menu.sh").read_text(encoding="utf-8")
        sleep = next(l for l in text.splitlines() if l.startswith('SAMSUNG_SLEEP_PACKAGES='))
        bloat = next(l for l in text.splitlines() if l.startswith('SAMSUNG_BLOAT_PACKAGES='))
        for want in ('com.zhiliaoapp.musically', 'com.estrongs.android.pop', 'com.moonshot.kimichat', 'com.android.vending'):
            self.assertIn(want, sleep)
        for keep in ('trackcatcher', 'audioconverter', 'com.suno', 'maxmpz', 'aimp', 'whatsapp', 'telegram',
                     'viber', 'sharelive', 'scloud', 'doramaland', 'renault', 'com.termux', 'anthropic'):
            self.assertNotIn(keep, sleep)
            self.assertNotIn(keep, bloat)
        for keep in ('com.android.vending', 'googlequicksearchbox', 'com.google.android.gms', 'sharelive', 'scloud'):
            self.assertNotIn(keep, bloat)
        self.assertIn('if target_is_samsung; then printf', text)
        for fn_name, nxt in (('set_bloat_enabled() {', 'set_apps_sleep() {'), ('set_apps_sleep() {', 'open_with_intent() {')):
            fn = text[text.index(fn_name):text.index(nxt)]
            self.assertIn('for pkg in $list; do', fn)
            self.assertNotIn('$BLOAT_PACKAGES', fn)
            self.assertNotIn('$SLEEP_PACKAGES', fn)

    def test_phone_diagnostics_audio_item_reads_sound_effects_only(self):
        repo = Path(__file__).resolve().parents[1]
        text = (repo / "tools" / "termux" / "phone-diagnostics-menu.sh").read_text(encoding="utf-8")
        fn = text[text.index('audio_snapshot() {'):text.index('memory_snapshot() {')]
        self.assertIn('dolby|atmos|soundalive', fn)
        self.assertIn('dumpsys media.audio_flinger', fn)
        for forbidden in ('settings put', 'pm disable', 'pm clear', 'force-stop'):
            self.assertNotIn(forbidden, fn)

    def test_phone_diagnostics_ip_prompt_prefills_network(self):
        repo = Path(__file__).resolve().parents[1]
        text = (
            repo / "tools" / "termux" / "phone-diagnostics-menu.sh"
        ).read_text(encoding="utf-8")

        self.assertIn('ask_target_ip() {', text)
        self.assertNotIn('prefix="192.168.50."', text)
        self.assertIn('ip -4 addr show wlan0', text)
        self.assertIn('read -e -i "$prefill"', text)
        # 2026-10-18: поле вводу має бути видимим — stderr у read не глушити.
        self.assertNotIn('answer 2>/dev/null', text)
        self.assertIn('echo "Адреса: $TARGET_IP" >&2', text)
        self.assertIn('prefill="192.168.0."', text)
        self.assertEqual(text.count('ask_target_ip\n'), 2)
        self.assertNotIn('192.168.x.x', text)

    def test_phone_diagnostics_saves_and_installs_all_apk_parts(self):
        repo = Path(__file__).resolve().parents[1]
        text = (
            repo / "tools" / "termux" / "phone-diagnostics-menu.sh"
        ).read_text(encoding="utf-8")

        self.assertIn('26) save_app_apks ;;', text)
        self.assertIn('27) install_saved_app ;;', text)
        save = text[text.index('save_app_apks() {'):text.index('install_saved_app() {')]
        self.assertIn('pm path $pkg', save)
        self.assertIn('adb -s "$ADB_TARGET" pull', save)
        self.assertIn('.apks', save)
        self.assertIn('SAI', save)
        # 2026-10-16: за замовчуванням — цей телефон (без ADB), вибір 2 — підключений.
        self.assertIn('Цей телефон', save)
        self.assertIn('case "${answer:-1}" in', save)
        self.assertIn('SRC="local"', save)
        self.assertIn('Показати всі встановлені застосунки', save)
        self.assertIn('cp "$path"', save)
        self.assertIn('require_connection', save[save.index('2) SRC="adb"'):])
        for forbidden in ('pm uninstall', 'pm clear', 'pm disable', 'adb -s "$ADB_TARGET" install',
                          'pm install', 'rm -rf'):
            self.assertNotIn(forbidden, save)
        inst = text[text.index('install_saved_app() {'):text.index('reset_target() {')]
        self.assertIn('install-multiple -r', inst)
        self.assertIn('Продовжити? Напиши так / ні', inst)
        for forbidden in ('uninstall', 'pm clear', ' -d ', 'rm -'):
            self.assertNotIn(forbidden, inst)

    def test_phone_diagnostics_connect_retries_and_explains(self):
        repo = Path(__file__).resolve().parents[1]
        text = (
            repo / "tools" / "termux" / "phone-diagnostics-menu.sh"
        ).read_text(encoding="utf-8")
        fn = text[text.index('connect_device() {'):text.index('show_connection() {')]
        self.assertIn('adb kill-server', fn)
        self.assertIn('*refused*)', fn)
        self.assertIn('*timed*|*"No route"*|*unreachable*)', fn)
        self.assertIn('Дозволити налагодження', fn)

    def test_phone_diagnostics_has_dedicated_installer(self):
        repo = Path(__file__).resolve().parents[1]
        text = (
            repo / "tools" / "termux" / "install-phone-diagnostics.sh"
        ).read_text(encoding="utf-8")

        self.assertIn('BASE_DIR="/storage/emulated/0/Documents/PhoneDiagnostics"', text)
        self.assertIn('INSTALL_DIR="$BASE_DIR/app"', text)
        self.assertIn('SHORTCUT_PATH="$SHORTCUT_DIR/Phone Diagnostics"', text)
        self.assertIn('SOURCE_PATH="tools/termux/phone-diagnostics-menu.sh"', text)
        self.assertIn('gh api "repos/$REPO/contents/$SOURCE_PATH"', text)
        self.assertIn('exec bash "$MENU_PATH"', text)
        self.assertIn('Renault Menu для цього не потрібне.', text)

    def test_ytm_helpers_are_compatibility_wrappers(self):
        repo = Path(__file__).resolve().parents[1]
        menu = (repo / "tools" / "termux" / "ytm-menu.sh").read_text(encoding="utf-8")
        build = (repo / "tools" / "termux" / "ytm-build-apk.sh").read_text(encoding="utf-8")
        download = (repo / "tools" / "termux" / "ytm-download-apk.sh").read_text(encoding="utf-8")

        self.assertIn('/Documents/YTM/tools/termux/ytm-menu.sh', menu)
        self.assertIn('/Documents/YTM/tools/termux/ytm-build-apk.sh', build)
        self.assertIn('/Documents/YTM/tools/termux/ytm-download-apk.sh', download)
        self.assertIn('exec bash "$YTM_MENU"', menu)
        self.assertIn('exec bash "$YTM_TOOL" "$@"', build)
        self.assertIn('exec bash "$YTM_TOOL" "$@"', download)
        self.assertNotIn('gh workflow run build-apk.yml', build)
        self.assertNotIn('gh run download "$RUN_ID"', download)

    def test_renault_apk_handoff_matches_phone_workflow(self):
        repo = Path(__file__).resolve().parents[1]
        build = (repo / "tools" / "termux" / "reno-build-apk.sh").read_text(encoding="utf-8")
        download = (repo / "tools" / "termux" / "reno-download-apk.sh").read_text(encoding="utf-8")
        opener = (repo / "tools" / "termux" / "reno-open-latest-apk.sh").read_text(encoding="utf-8")

        self.assertIn('gh workflow run "$WORKFLOW"', build)
        self.assertIn('gh run watch "$RUN_ID"', build)
        self.assertIn('reno-download-apk.sh" "$RUN_ID"', build)

        self.assertIn('android/app/build.gradle.kts', download)
        self.assertIn('versionName[[:space:]]*=', download)
        self.assertIn('EXPECTED_ARTIFACT="Renault-Docs-v${VERSION}-Debug"', download)
        self.assertIn('reno_find_compatible_android_run', download)
        self.assertIn('reno_android_build_compatible', download)
        self.assertIn('actions/runs/$RUN_ID/artifacts?per_page=100', download)
        self.assertIn(
            'Старішу версію з іншим Android-кодом або artifact з іншої гілки НЕ завантажую.',
            download,
        )
        self.assertIn('if [ "$RUN_BRANCH" != "$BRANCH" ]; then', download)
        self.assertIn('if [ "$RUN_SHA" != "$HEAD_SHA" ]; then', download)
        self.assertIn('gh run download "$RUN_ID"', download)
        self.assertIn('--name "$EXPECTED_ARTIFACT"', download)
        self.assertIn('sha256sum -c', download)
        self.assertIn('/storage/emulated/0/Documents/Renault/packages/Renault-Docs-v', download)
        self.assertIn('Renault-Docs-v${VERSION}-debug.apk', download)
        self.assertIn('reno-open-latest-apk.sh" "$PHONE_DIR"', download)

        self.assertIn('Renault-Docs-v*-build', opener)
        self.assertIn('android.intent.action.VIEW', opener)
        self.assertIn('android.intent.action.OPEN_DOCUMENT_TREE', opener)
        self.assertIn('android.provider.extra.INITIAL_URI', opener)


    def test_termux_shell_scripts_have_valid_bash_syntax(self):
        repo = Path(__file__).resolve().parents[1]
        scripts = [
            repo / "menu.sh",
            repo / "tools" / "install_termux_aliases.sh",
            repo / "tools" / "termux" / "reno-github.sh",
            repo / "tools" / "termux" / "reno-build-apk.sh",
            repo / "tools" / "termux" / "reno-download-apk.sh",
            repo / "tools" / "termux" / "reno-candidate.sh",
            repo / "tools" / "termux" / "reno-project-status.sh",
            repo / "tools" / "termux" / "reno-restore-accepted-signer.sh",
            repo / "tools" / "termux" / "reno-check-dataset-links.sh",
            repo / "tools" / "termux" / "reno-quarantine-legacy-android.sh",
        ]

        for script in scripts:
            with self.subTest(script=script):
                subprocess.run(
                    ["bash", "-n", str(script)],
                    check=True,
                    capture_output=True,
                    text=True,
                )

    def test_apk_helpers_follow_current_origin_repository(self):
        repo = Path(__file__).resolve().parents[1]
        helper = (
            repo / "tools" / "termux" / "reno-github.sh"
        ).read_text(encoding="utf-8")

        self.assertIn('git remote get-url origin', helper)
        self.assertIn('RENAULT_GH_REPO', helper)

        for name in ("reno-build-apk.sh", "reno-download-apk.sh", "reno-candidate.sh"):
            text = (repo / "tools" / "termux" / name).read_text(encoding="utf-8")
            self.assertIn('GH_REPO="$(reno_github_repo)"', text)
            self.assertNotIn('GH_REPO="faric-ua/renault-docs-android"', text)


    def test_project_status_helper_shows_project_and_build_state(self):
        repo = Path(__file__).resolve().parents[1]
        text = (
            repo / "tools" / "termux" / "reno-project-status.sh"
        ).read_text(encoding="utf-8")

        self.assertIn("Renault Docs · Статус", text)
        self.assertIn('show_apk_state', text)
        self.assertIn('show_tests_state', text)
        self.assertIn('reno_find_compatible_android_run', text)
        self.assertIn("✓ МОЖНА ЗАВАНТАЖУВАТИ", text)
        self.assertIn("Натисни: 8", text)
        self.assertNotIn("CURRENT_PLAN.md", text)
        self.assertNotIn("RELEASE_META.json", text)
        self.assertNotIn("SIGNER_CONTINUITY_PENDING", text)


    def test_restore_accepted_signer_helper_is_private_archive_to_github_secrets_only(self):
        repo = Path(__file__).resolve().parents[1]
        text = (
            repo / "tools" / "termux" / "reno-restore-accepted-signer.sh"
        ).read_text(encoding="utf-8")

        self.assertIn("renault-docs-android-private-archive", text)
        self.assertIn("EXPECTED_KEYSTORE_SHA256", text)
        self.assertIn("EXPECTED_CERT_SHA256", text)
        self.assertIn("gh secret set RENAULT_DEV_KEYSTORE_B64", text)
        self.assertIn("gh secret set RENAULT_DEV_CERT_SHA256", text)
        self.assertIn("Секретні значення на екран НЕ виводяться.", text)
        self.assertIn('exec bash "$REPO_DIR/tools/termux/reno-build-apk.sh"', text)
        self.assertNotIn("cat $TMP_DIR/renault-docs-dev.jks", text)
        self.assertNotIn("echo $STORE_PASSWORD", text)
        self.assertNotIn("echo $KEY_PASSWORD", text)


    def test_phone_diagnostics_media_scan_report_only_reads(self):
        # 2026-10-15: M30s — сканер медіа годинами тримав ~1 ядро (знімки 068, 069).
        repo = Path(__file__).resolve().parents[1]
        text = (repo / "tools" / "termux" / "phone-diagnostics-menu.sh").read_text(encoding="utf-8")
        self.assertIn("28) media_scan_report ;;", text)
        self.assertIn('echo "28 — Що робить сканер медіа → файл (нічого не змінює)"', text)
        fn = text[text.index("media_scan_report() {"):text.index("# media_scan_report end")]
        self.assertIn("logcat -d", fn)
        self.assertIn("-mmin -30", fn)
        self.assertIn("top -b -n 1 -H", fn)
        for forbidden in ("am force-stop", "pm disable", "pm clear", "settings put", "rm ", "reboot", "input ", "-delete"):
            self.assertNotIn(forbidden, fn)

    def test_phone_diagnostics_log_path_repeated_at_end(self):
        # 2026-10-15 (faric): шлях до лога — і на початку, і в кінці роботи пункту.
        repo = Path(__file__).resolve().parents[1]
        text = (repo / "tools" / "termux" / "phone-diagnostics-menu.sh").read_text(encoding="utf-8")
        fn = text[text.index("pause_menu() {"):text.index("detect_phone_ip() {")]
        self.assertIn('"${CURRENT_LOG:-}"', fn)
        self.assertIn('echo "  $CURRENT_LOG"', fn)
        self.assertIn('CURRENT_LOG=""', fn)

    def test_phone_diagnostics_gallery_rescan_never_clears_data(self):
        # 2026-10-15 (faric): прибрати з Галереї папки з .nomedia без чистки даних
        # Галереї/Медіасховища (там кошик Галереї й плейлисти).
        repo = Path(__file__).resolve().parents[1]
        text = (repo / "tools" / "termux" / "phone-diagnostics-menu.sh").read_text(encoding="utf-8")
        self.assertIn("29) gallery_nomedia_rescan ;;", text)
        self.assertIn('echo "29 — Галерея: прибрати папки з .nomedia (файли не чіпає)"', text)
        fn = text[text.index("gallery_count_in_index() {"):text.index("# gallery_nomedia_rescan end")]
        self.assertIn("MEDIA_SCANNER_SCAN_FILE", fn)
        self.assertIn("content query", fn)
        self.assertIn("так|Так|ТАК|y|Y", fn)
        for forbidden in ("pm clear", "--cache-only", "trim-caches", "am force-stop", "rm ", "-delete", "reboot", "settings put"):
            self.assertNotIn(forbidden, fn)

    def test_phone_diagnostics_crypto_prices_only_public_history(self):
        # 2026-10-15 (faric): ціни для «Крипто-тренажера» — лише публічні свічки,
        # без ключів і без торгівлі.
        repo = Path(__file__).resolve().parents[1]
        text = (repo / "tools" / "termux" / "phone-diagnostics-menu.sh").read_text(encoding="utf-8")
        self.assertIn("30) crypto_prices ;;", text)
        fn = text[text.index("crypto_prices() {"):text.index("# crypto_prices end")]
        self.assertIn("/api/v3/klines?symbol=", fn)
        self.assertIn(".nomedia", fn)
        for forbidden in ("X-MBX-APIKEY", "/api/v3/order", "signature=", "secret", "adb "):
            self.assertNotIn(forbidden, fn)

    def test_phone_diagnostics_app_threads_only_reads(self):
        # 2026-10-16 (faric): Audio Converter на M30s кодує ~1x реального часу —
        # дивимось потоки (ядра, nice, cpuset), нічого не змінюючи.
        repo = Path(__file__).resolve().parents[1]
        text = (repo / "tools" / "termux" / "phone-diagnostics-menu.sh").read_text(encoding="utf-8")
        self.assertIn("31) app_threads_report ;;", text)
        self.assertIn('echo "31 — Що роблять потоки застосунку (ядра, пріоритет) → файл"', text)
        fn = text[text.index("pick_faric_app() {"):text.index("# app_threads_report end")]
        self.assertIn("ua.faric.audioconverter", fn)
        self.assertIn("ua.faric.trackcatcher", fn)
        self.assertIn("top -b -n 2 -d 5 -H -p", fn)
        self.assertIn("/cpuset", fn)
        for forbidden in ("am force-stop", "pm disable", "pm clear", "settings put", "rm ", "reboot", "input ", "renice", "cmd package"):
            self.assertNotIn(forbidden, fn)

    def test_phone_diagnostics_app_compile_keeps_data(self):
        # 2026-10-16 (лог 092): «Jit thread pool» 99 % поруч з кодуванням MP3 —
        # повна компіляція застосунку, без чистки даних і перевстановлення.
        repo = Path(__file__).resolve().parents[1]
        text = (repo / "tools" / "termux" / "phone-diagnostics-menu.sh").read_text(encoding="utf-8")
        self.assertIn("32) app_compile_speed ;;", text)
        self.assertIn('echo "32 — Прискорити застосунок (машинний код; звук і дані ті самі)"', text)
        fn = text[text.index("app_compile_status() {"):text.index("# app_compile_speed end")]
        self.assertIn("cmd package compile -m speed -f", fn)
        self.assertIn("cmd package compile --reset", fn)
        self.assertIn("так|Так|ТАК|y|Y", fn)
        for forbidden in ("pm clear", "uninstall", "am force-stop", "rm ", "reboot", "settings put", "--delete", "bg-dexopt-job"):
            self.assertNotIn(forbidden, fn)

    def test_phone_diagnostics_crash_report_only_reads(self):
        # 2026-10-16 (faric): Audio Converter закривається після «Стоп» — журнал падінь.
        repo = Path(__file__).resolve().parents[1]
        text = (repo / "tools" / "termux" / "phone-diagnostics-menu.sh").read_text(encoding="utf-8")
        self.assertIn("34) app_crash_report ;;", text)
        self.assertIn('echo "34 — Чому закрився застосунок (журнал падінь) → файл"', text)
        fn = text[text.index("app_crash_report() {"):text.index("# app_crash_report end")]
        self.assertIn("logcat -d -b crash", fn)
        self.assertIn("exit-info", fn)
        self.assertIn("pick_faric_app", fn)
        for forbidden in ("logcat -c", "am force-stop", "pm clear", "cmd package", "rm ", "reboot", "settings put"):
            self.assertNotIn(forbidden, fn)

    def test_phone_diagnostics_item13_stops_installed_media_module(self):
        # 2026-10-15: на M30s пакет сканера може бути без «google».
        repo = Path(__file__).resolve().parents[1]
        text = (repo / "tools" / "termux" / "phone-diagnostics-menu.sh").read_text(encoding="utf-8")
        fn = text[text.index("force_stop_google_media() {"):text.index("force_stop_samsung_media() {")]
        self.assertIn("com.google.android.providers.media.module", fn)
        self.assertIn("com.android.providers.media.module", fn)
        self.assertIn("pm path", fn)

    def test_phone_diagnostics_open_with_intent_only_reads(self):
        # 2026-10-14: what «Мої файли» sends for .rdproject (dat=…, typ=…).
        repo = Path(__file__).resolve().parents[1]
        text = (repo / "tools" / "termux" / "phone-diagnostics-menu.sh").read_text(encoding="utf-8")
        self.assertIn("25) open_with_intent ;;", text)
        self.assertIn('echo "25 — Що надсилає «Відкрити за допомогою» → файл"', text)
        fn = text[text.index("open_with_intent() {"):text.index("save_app_apks() {")]
        self.assertIn("dumpsys activity activities", fn)
        self.assertIn("logcat -d", fn)
        for forbidden in ("pm ", "am force-stop", "settings put", "rm -", "reboot", "input "):
            self.assertNotIn(forbidden, fn)


if __name__ == "__main__":
    unittest.main()
