// AGP 9 compiles Kotlin itself ("built-in Kotlin"), so there is no org.jetbrains.kotlin.android plugin here.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "ca.georgebrown.studygroupfinder"
    compileSdk = 35

    defaultConfig {
        applicationId = "ca.georgebrown.studygroupfinder"
        minSdk = 26 // Android 8.0, as stated in the design proposal (Section 9)
        targetSdk = 35
        versionCode = 2 // Incremented sequentially per Sept 14 lecture notes
        versionName = "0.2.0-functional"

        // Maps API key placeholder pattern from Oct 16 Lab Notes (Section 13.1 & Appendix B)
        manifestPlaceholders["MAPS_API_KEY"] =
            (project.findProperty("MAPS_API_KEY") as String?) ?: "YOUR_API_KEY"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.coroutines.play.services)

    // Firebase Authentication & Cloud Firestore (Section 8)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.compose.ui.tooling)
}