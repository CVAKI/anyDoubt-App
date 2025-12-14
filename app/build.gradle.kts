plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.humangodcvaki.anydoubt"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.humangodcvaki.anydoubt"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.generativeai)

    // Material Icons Extended - REQUIRED for PhoneAndroid, CloudDownload, etc.
    implementation("androidx.compose.material:material-icons-extended:1.7.6")

    // AppCompat for XML themes
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Firebase BOM (Bill of Materials) - manages all Firebase versions
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))

    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth-ktx")

    // Google Sign In for Authentication
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Firebase Analytics (optional but recommended)
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation(libs.androidx.compose.foundation.android)

    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))

    // Firebase Realtime Database
    implementation("com.google.firebase:firebase-database-ktx")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    implementation("io.coil-kt:coil-compose:2.5.0")

    // For JSON parsing
    implementation("org.json:json:20230227")

    // Ads
    implementation("com.google.android.gms:play-services-ads:22.6.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
configurations.all {
    resolutionStrategy {
        force("com.google.android.gms:play-services-measurement-api:22.1.2")

    }
}