plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ── Auto-download sherpa-onnx AAR on first build ──────────────────────────────
val sherpaVersion = "1.13.8"
val sherpaAar = layout.projectDirectory.file("libs/sherpa-onnx-android.aar")

val downloadSherpaOnnx by tasks.registering(Exec::class) {
    sherpaAar.asFile.parentFile.mkdirs()
    onlyIf { !sherpaAar.asFile.exists() }
    commandLine(
        "curl", "-L", "--progress-bar",
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/" +
        "v$sherpaVersion/sherpa-onnx-$sherpaVersion.aar",
        "-o", sherpaAar.asFile.absolutePath
    )
}

afterEvaluate {
    tasks.named("preBuild") { dependsOn(downloadSherpaOnnx) }
}

android {
    namespace = "com.sanjeevani"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sanjeevani"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    testOptions { unitTests.isIncludeAndroidResources = true }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // Prevent aapt from double-compressing the already-zipped Kokoro model
    androidResources {
        @Suppress("DEPRECATION")
        noCompress += "zip"
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    // Kokoro TTS via sherpa-onnx (auto-downloaded by downloadSherpaOnnx task below)
    implementation(files("libs/sherpa-onnx-android.aar"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // MediaPipe Tasks
    implementation(libs.mediapipe.tasks.vision)
    implementation(libs.mediapipe.tasks.genai)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation("androidx.appcompat:appcompat:1.7.0")

    debugImplementation(libs.androidx.ui.tooling)
}
