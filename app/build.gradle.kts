plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing is taken from the environment when it is available, so the
// same build works with CI secrets, a local keystore, or neither.
val releaseKeystore: File? = System.getenv("REALBUDS_KEYSTORE")
    ?.takeIf { it.isNotBlank() }
    ?.let(::file)
    ?.takeIf { it.exists() }

android {
    namespace = "com.realbuds.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.realbuds.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    // One APK per ABI plus a universal fallback. Native code here is only the
    // Compose/AndroidX runtime, but splitting still trims each download, and the
    // universal APK stays available for anyone unsure of their device.
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("REALBUDS_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("REALBUDS_KEY_ALIAS")
                keyPassword = System.getenv("REALBUDS_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Falls back to the debug key when no keystore is supplied, so a
            // plain `assembleRelease` still works on a fresh clone.
            signingConfig = signingConfigs.getByName(
                if (releaseKeystore != null) "release" else "debug"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
}
