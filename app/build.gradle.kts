import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/**
 * Release signing credentials. Loaded from `<repo>/keystore.properties` (which
 * is gitignored) and falls back to the matching `FORSETI_*` env vars so CI can
 * inject the same values without writing a file to disk.
 *
 * The properties file must define:
 *   storeFile=/absolute/path/to/forseti-upload.jks
 *   storePassword=...
 *   keyAlias=forseti-upload
 *   keyPassword=...
 *
 * If none of these are present (typical for a contributor cloning the repo)
 * the release build falls back to the debug signing config so local
 * `assembleRelease` smoke tests still work — Play uploads must use the real
 * keystore, of course.
 */
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}
fun signingValue(key: String, envName: String): String? =
    keystoreProps.getProperty(key) ?: System.getenv(envName)

val releaseStoreFile = signingValue("storeFile", "FORSETI_KEYSTORE_FILE")
val releaseStorePassword = signingValue("storePassword", "FORSETI_KEYSTORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "FORSETI_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "FORSETI_KEY_PASSWORD")
val releaseSigningConfigured = listOf(
    releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword
).all { !it.isNullOrBlank() } && file(releaseStoreFile!!).exists()

android {
    namespace = "com.forseti"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.forseti"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Bundle full native debug symbols (.so) for transitive native
            // libs (ML Kit, SQLite, PDF renderer) into the AAB so Play
            // Console can symbolicate native crash/ANR reports. The symbols
            // do not ship to user devices — Play strips them from the
            // per-device splits — they only live in the AAB upload.
            ndk {
                debugSymbolLevel = "FULL"
            }
            signingConfig = if (releaseSigningConfigured) {
                signingConfigs.getByName("release")
            } else {
                // No upload keystore on this machine: fall back to debug
                // signing so a local `assembleRelease` still produces an
                // installable APK for smoke tests. Play uploads will still
                // require the real keystore via keystore.properties or
                // FORSETI_* env vars.
                signingConfigs.getByName("debug")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    androidResources {
        // Don't compress PDFs - allows AssetFileDescriptor random access for PdfRenderer.
        noCompress += listOf("pdf")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    // Ktor depends on slf4j-api only; release R8 needs a binding (StaticLoggerBinder).
    implementation(libs.slf4j.android)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.mlkit.text.recognition)

    implementation(libs.billing.ktx)
    implementation(libs.androidx.documentfile)

    // PDF rendering uses android.graphics.pdf.PdfRenderer (framework, API 21+).
    // No third-party PDF native lib — eliminates 16 KB ELF-alignment problems
    // on Android 15+ (Pixel 9 series and later) and shrinks the APK.

    implementation(libs.markdown.renderer)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
