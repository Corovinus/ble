rootProject.name = "YourAppName"
include(":app")

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        // ← must match the last published compiler under androidx.compose.compiler
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    kotlinOptions {
        // match the JVM target suggested by the compatibility table
        jvmTarget = "19"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.material3:material3:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.0")

    implementation("com.google.protobuf:protobuf-javalite:3.21.12")
    implementation("com.google.protobuf:protobuf-java:3.21.12")
}
