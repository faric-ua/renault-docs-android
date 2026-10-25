import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val renaultDevKeystorePath =
    providers.environmentVariable("RENAULT_DEV_KEYSTORE_PATH").orNull
val renaultDevStorePassword =
    providers.environmentVariable("RENAULT_DEV_STORE_PASSWORD").orNull
val renaultDevKeyAlias =
    providers.environmentVariable("RENAULT_DEV_KEY_ALIAS").orNull
val renaultDevKeyPassword =
    providers.environmentVariable("RENAULT_DEV_KEY_PASSWORD").orNull

val renodocsWebClientId =
    providers.gradleProperty("renodocs.googleWebClientId").orNull?.trim().orEmpty()

val renaultDevKeystoreFile = renaultDevKeystorePath
    ?.takeIf { it.isNotBlank() }
    ?.let { file(it) }

val renaultStableDebugSigningAvailable =
    renaultDevKeystoreFile?.exists() == true &&
        !renaultDevStorePassword.isNullOrBlank() &&
        !renaultDevKeyAlias.isNullOrBlank() &&
        !renaultDevKeyPassword.isNullOrBlank()

android {
    namespace = "com.saney.renaultdocs"
    compileSdk = 36

    defaultConfig {
        // 0.6.0 (owner 2026-10-18): Renodocs is a separate app beside Renault Docs 0.5.153,
        // so its own package. Kotlin namespace stays com.saney.renaultdocs.
        // 0.6.5 (owner 2026-10-18 17:37–17:38): final package com.farsan.renodocs (was
        // com.saney.renodocs in 0.6.0–0.6.4) — installs as a new app; never rename again
        // (a new package = a new app with empty volumes; Google Play forbids changing it).
        applicationId = "com.farsan.renodocs"
        minSdk = 26
        targetSdk = 36
        versionCode = 180
        versionName = "0.6.10"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // v0.5.150: update from the author's Google Drive (Settings → «Оновлення застосунку»).
        // A Google Play build must set this to false and drop REQUEST_INSTALL_PACKAGES:
        // Play forbids apps that update themselves outside Play.
        buildConfigField("boolean", "DRIVE_UPDATES", "true")

        // 0.6.5 (#133 «вхід через Google»): Web client ID of the owner's Google Cloud project.
        // Not a secret (Google client IDs are public). Empty = the card says sign-in is not set up.
        // The Android OAuth client in the same project: package com.farsan.renodocs + SHA-1 of the
        // stable debug key (docs/plans/2026-10-18_GOOGLE_SIGN_IN_SETUP.md).
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$renodocsWebClientId\"")
    }

    signingConfigs {
        create("stableDebug") {
            if (renaultStableDebugSigningAvailable) {
                storeFile = renaultDevKeystoreFile
                storePassword = renaultDevStorePassword
                keyAlias = renaultDevKeyAlias
                keyPassword = renaultDevKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            if (renaultStableDebugSigningAvailable) {
                signingConfig = signingConfigs.getByName("stableDebug")
            }
        }

        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("org.jspecify:jspecify:1.0.0")
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("org.tukaani:xz:1.10")
    implementation("com.github.junrar:junrar:8.1.1")
    // 0.6.5: «Увійти через Google» (Android Credential Manager).
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    // 0.6.6 (#133 closed catalogue): AuthorizationClient — permission «read files on Drive».
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
