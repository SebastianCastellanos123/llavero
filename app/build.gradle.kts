plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.sebastiancastellanos123.llavero"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.sebastiancastellanos123.llavero"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // La clave de firma nunca va en el repositorio: GitHub Actions la entrega por
    // variables de entorno desde sus secrets (ver .github/workflows/release.yml).
    val releaseKeystore = System.getenv("LLAVERO_KEYSTORE_PATH")
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("LLAVERO_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("LLAVERO_KEY_ALIAS")
                keyPassword = System.getenv("LLAVERO_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = when {
                releaseKeystore != null -> signingConfigs.getByName("release")
                // Solo para probar en local el build optimizado: ./gradlew assembleRelease -Pllavero.debugSigning
                providers.gradleProperty("llavero.debugSigning").isPresent -> signingConfigs.getByName("debug")
                else -> null
            }
            // R8 sobre toda la app. La plantilla limitaba la optimización a androidx/kotlin
            // (packageScope) y eso dejaba clases movidas sin acceso a su clase padre:
            // la app se cerraba al abrir con IllegalAccessError.
            optimization {
                enable = true
            }
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
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.zxing.core)
    implementation(libs.androidx.biometric)
    // Versión explícita: biometric trae fragment 1.2.5, que no acepta los códigos de
    // solicitud de los selectores modernos (ActivityResultRegistry) y cierra la app.
    implementation(libs.androidx.fragment)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}