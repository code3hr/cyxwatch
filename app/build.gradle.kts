plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

import java.io.File

val releaseKeystorePath = System.getenv("CYXWATCH_RELEASE_KEYSTORE_PATH")
    ?: System.getenv("CYXWATCH_KEYSTORE_PATH")
    ?: project.findProperty("cyxwatch.release.keystore.path")?.toString()

val releaseKeystorePassword = System.getenv("CYXWATCH_RELEASE_KEYSTORE_PASSWORD")
    ?: System.getenv("CYXWATCH_KEYSTORE_PASSWORD")
    ?: project.findProperty("cyxwatch.release.keystore.password")?.toString()

val releaseKeystoreAlias = System.getenv("CYXWATCH_RELEASE_KEY_ALIAS")
    ?: System.getenv("CYXWATCH_KEY_ALIAS")
    ?: project.findProperty("cyxwatch.release.keystore.alias")?.toString()

val releaseKeyPassword = System.getenv("CYXWATCH_RELEASE_KEY_PASSWORD")
    ?: System.getenv("CYXWATCH_KEY_PASSWORD")
    ?: project.findProperty("cyxwatch.release.key.password")?.toString()

val releaseKeystoreFile = releaseKeystorePath?.trim()?.let { File(it) }
val hasExplicitReleaseKeystore = releaseKeystoreFile?.exists() == true &&
    !releaseKeystorePath.isNullOrBlank() &&
    !releaseKeystorePassword.isNullOrBlank() &&
    !releaseKeystoreAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "com.cyxwatch.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cyxwatch.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 4
        versionName = "0.0.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasExplicitReleaseKeystore) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeystoreAlias
                keyPassword = releaseKeyPassword
            }
        }

        if (!hasExplicitReleaseKeystore) {
            create("releaseDebugFallback") {
                initWith(signingConfigs.getByName("debug"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = when {
                hasExplicitReleaseKeystore -> signingConfigs.getByName("release")
                signingConfigs.names.contains("releaseDebugFallback") -> signingConfigs.getByName("releaseDebugFallback")
                else -> null
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.security:security-crypto:1.0.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
