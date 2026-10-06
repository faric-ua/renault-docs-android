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
        applicationId = "com.saney.renaultdocs"
        minSdk = 26
        targetSdk = 36
        versionCode = 83
        versionName = "0.5.67"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
