plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.filmflip"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.filmflip"
        minSdk = 28
        targetSdk = 36
        versionCode = 4
        versionName = "1.03"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystoreFile = project.layout.projectDirectory.file("../filmflip-release.jks").asFile
            val storePass = findProperty("KEYSTORE_PASSWORD") as? String ?: System.getenv("KEYSTORE_PASSWORD")
            val keyPass = findProperty("KEY_PASSWORD") as? String ?: System.getenv("KEY_PASSWORD")
            if (keystoreFile.exists() && !storePass.isNullOrEmpty()) {
                storeFile = keystoreFile
                storePassword = storePass
                keyAlias = "filmflip"
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            if (signingConfigs.getByName("release").storeFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
            excludes += "**/libandroidx.graphics.path.so"
            excludes += "**/libimage_processing_util_jni.so"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.ui.graphics)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}