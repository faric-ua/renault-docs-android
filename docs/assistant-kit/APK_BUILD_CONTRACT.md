# Renault Docs — APK Build Contract

Цей контракт активується, коли у `android/` з'явиться реальний Gradle application module.

Він адаптований з перевіреного YTM Importer release pipeline.

## Початкова toolchain baseline

Плановий baseline:
- Java/JVM 17;
- compileSdk 36;
- targetSdk 36;
- minSdk 26 як стартова сумісність, якщо залежності Renault viewer не вимагатимуть вищий рівень;
- release build з R8/ProGuard;
- BuildConfig дозволений лише якщо реально потрібен.

Версії залежностей не копіюються сліпо: перед першим Android scaffold перевіряємо актуальні Android Gradle Plugin/Kotlin/AndroidX WebKit/PDF залежності.

## Signing

### Development/debug APK

Debug APK, який ставиться на телефон під час розробки, повинен мати **стабільний development certificate**. Інакше кожен ephemeral GitHub runner створює новий default debug keystore, і Android блокує оновлення поверх попереднього APK через signature mismatch.

Для development build у приватному repository дозволений окремий **не-production** key payload:
`.github/signing/renault-docs-dev.jks.b64`.

Його контракт:
- використовується тільки для debug/development APK;
- пароль/alias не є production secret;
- SHA-256 самого keystore перевіряється у CI;
- SHA-256 signer certificate перевіряється через `apksigner`;
- цей key ніколи не використовується для production release.

Поточний development signer certificate SHA-256:

```text
dd588fbb3093a047f81c24397fc6d2ab7f5a32040425a21dd24506cce59a9802
```

### Production release APK

Production/release keystore і passwords **ніколи не комітяться**.

У Git допускається лише example properties без реальних production secrets.

GitHub Actions secrets:
- base64 release keystore;
- store password;
- key alias;
- key password.

## Signed APK pipeline

Release workflow повинен:

1. checkout exact ref;
2. запускати release preflight;
3. запускати JVM/unit tests;
4. setup Java 17;
5. setup Gradle;
6. знайти Android SDK;
7. встановити required platform/build-tools;
8. перевірити signing secrets;
9. відновити keystore тільки у CI runtime;
10. зібрати `:app:assembleRelease`;
11. перейменувати APK у versioned filename;
12. перевірити `apksigner`;
13. перевірити `zipalign -c -v 4`;
14. перевірити package/sdk metadata через `aapt`;
15. створити SHA-256;
16. upload APK + checksum як один artifact.

## Artifact naming

```text
Renault-Docs-vX.Y.Z-release.apk
Renault-Docs-vX.Y.Z-release.apk.sha256
```

Phone folder:

```text
/storage/emulated/0/Documents/Renault/packages/Renault-Docs-vX.Y.Z-build/
```

APK не розкидати випадково в root Download.

## Build ≠ phone PASS

Успішний signed APK означає лише:
- source compiled;
- tests/preflight passed;
- signing worked;
- APK structure/signature verified.

Це не доводить:
- rotation;
- dialogs;
- WebView behavior;
- PDF rendering;
- SAF permissions;
- real device navigation.

Для цього потрібен окремий phone QA.

## Security gates

Release preflight повинен блокувати:
- tracked keystore;
- tracked signing properties;
- secrets/tokens;
- unexpected package id/version;
- відсутню signature verification;
- відсутню zipalign verification;
- відсутній SHA-256;
- release docs, якщо release workflow їх вимагає.
