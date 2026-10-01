plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace   = "de.tou.wastesort"
    compileSdk  = 35

    defaultConfig {
        applicationId = "de.tou.wastesort"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = 1
        versionName   = "1.0-thesis"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // QR-Code-Scanning (ML Kit, nutzt die vorhandene CameraX-ImageAnalysis-Pipeline)
    implementation(libs.mlkit.barcode.scanning)

    // Image loading
    implementation(libs.coil.compose)

    // HTTP (fuer die dev-mode-only TrashAI-Live-Anbindung, nicht im Studien-Workflow)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.ui.tooling)

    // Unit-Tests (JVM, kein Android-Geraet noetig)
    testImplementation(libs.junit)
}
