plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.subradar"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.subradar"
        minSdk = 26
        targetSdk = 35
        versionCode = 2000
        versionName = "2.0.0.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.3.20"))
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("top.yukonga.miuix.kmp:miuix-android:0.8.8")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
