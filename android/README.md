# Renault Docs Android

Android project root: this `android/` directory.

## v0.1.0 milestone

Перший APK навмисно перевіряє фундамент:

- Library screen;
- Android SAF folder picker;
- persistable read URI;
- validation of `renault-dataset.json`;
- dataset tile;
- persistence across app restart;
- Viewer placeholder;
- Back/rotation contract.

Ще **не реалізовано** у v0.1.0:

- конвертація старої Renault папки всередині APK;
- production legacy WebView;
- Android PDF layer;
- release signing.

## Build

Toolchain baseline copied from the already used YTM project:

- Java 17;
- Gradle 8.13;
- Android Gradle Plugin 8.13.2;
- Kotlin 2.3.21;
- compileSdk/targetSdk 36;
- minSdk 26.

From repository root:

```bash
gradle -p android :app:testDebugUnitTest
gradle -p android :app:assembleDebug
```

GitHub Actions workflow:

```text
.github/workflows/android-debug.yml
```

It builds an installable debug APK and verifies it with `apksigner`, `zipalign`, `aapt` and SHA-256.

## Storage model

No `MANAGE_EXTERNAL_STORAGE`.

The app opens a dataset through `ACTION_OPEN_DOCUMENT_TREE` and stores a persistable read URI.

Expected folder root:

```text
renault-dataset.json
_renault/
  START.html
...
```

## Phone QA

See:

```text
docs/v.0.1.0/qa/PHONE_TEST.md
```
