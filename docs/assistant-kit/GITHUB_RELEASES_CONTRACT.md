# Renault Docs — verified GitHub Releases

## Why

Actions artifacts have a short retention window (currently **3 days** in `.github/workflows/android-debug.yml`). Users need a durable, auditable APK download. GitHub Release assets persist until the repository owner deletes them; they **do not** cause installed copies to expire.

**The repository is public**. Release APK and checksum are public too; never attach any source user data, proprietary Renault raw archives, backups or signing keys.

## Explicit one-version promotion

1. Build the app via the existing trusted `Android Debug APK` workflow on `main`. This workflow verifies the **stable developer signing certificate**, APK alignment, and generates the original APK's SHA-256.
2. After a successful build, review `docs/v.<version>/RELEASE_META.json`, QA status and run evidence. Commit exactly one `docs/release-promotions/v<version>.json` via a reviewed PR.
3. A push to `main` affecting that manifest triggers `publish-verified-apk.yml`. A manual dispatch with an existing checked-in promotion file also works. **Regular app pushes never trigger publishing.**
4. The publisher checks repository/main identity, full source SHA and ancestor, *version at the original build commit*, successful stable `Android Debug APK` run on `main`, immutable Actions artifact ID/name/bundle digest, original APK checksum, and APK ZIP structure.
5. Publish the original signed APK and its `.sha256` to `vX.Y.Z-debug` with `prerelease=true`. Do not rebuild, resign, or substitute an ephemeral PR APK. Never overwrite an existing tag/release silently.

GitHub artifact bundle digest is **not** necessarily the inner APK checksum; publish the latter alongside the file. Keep versionCode/source SHA in the release description.

## User safety

- This is a **developer-signed debug prerelease** (not a store release).
- When installing over an existing app, use the same signer, keep the package ID and install over; do not uninstall or clear data.
- Keep the authentic Classic viewer and existing project/volume storage untouched.
- Publishing must not mark phone QA closed. For v0.5.83 only core New Volume route received a user PASS; expanded QA remains on hold.
- A GitHub release normally has no short Actions retention timer, but continued access still depends on the GitHub repo and owner retaining the release.
- Do not automatically mark every successful main APK as a public release; promotion is explicit per version.

## First promotion

`docs/release-promotions/v0.5.83.json` promotes exactly v0.5.83/build 99 from signed Actions run ID `37853352879` (run #146), source `b5f1c22d73642d7341943e4320b5fb469cbe9bec`, artifact ID `11582942367`. QA: core PHONE PASS, other QA paused.

## Termux menu 8 compatibility fallback (2026-10-09)

`reno-download-apk.sh` still prefers its exact current-code GitHub Actions run. If that artifact is expired/missing and the current branch is `main`, `reno-download-release-apk.sh` tries `vX.Y.Z-debug`:
- requires repository `faric-ua/renault-docs-android` and the reviewed local `docs/release-promotions/vX.Y.Z.json`;
- checks release is published prerelease, its target SHA matches the promotion's signed source SHA, and the source is an **ancestor with identical Android code** to current checkout;
- downloads APK **and .sha256**, verifies checksum before and after copying to the existing `Documents/Renault/packages/Renault-Docs-vX.Y.Z-build` folder;
- opens only that output folder, never deletes source files, never uninstalls or clears the app;
- explicit Actions run-ID downloads and candidate PR branches retain original behavior, with no cross-branch Release substitution.

This is a **Termux tooling-only change**, not a new installed Android version. If no compatible Release exists, the original safe no-artifact diagnostic remains. Automated shell/static contract tests cover the fallback; device execution remains intentionally on hold.
