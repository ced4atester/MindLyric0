import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.mindlyric.mindlyric0"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.mindlyric.mindlyric0"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // local.properties dosyasını manuel olarak okuyoruz
        // (project.findProperty sadece gradle.properties'i okur, local.properties'i okumaz)
        val localProps = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localProps.load(localPropsFile.inputStream())
        }
        val apiKey = localProps.getProperty("CLAUDE_API_KEY") ?: ""
        buildConfigField("String", "CLAUDE_API_KEY", "\"$apiKey\"")

        // OpenWeatherMap API anahtarı
        val weatherApiKey = localProps.getProperty("WEATHER_API_KEY") ?: ""
        buildConfigField("String", "WEATHER_API_KEY", "\"$weatherApiKey\"")
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

    // BuildConfig aktif ediyoruz (API key buradan okunacak)
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    // Claude API ile HTTP isteği yapmak için OkHttp
    implementation(libs.okhttp)
    // Ruh hali trend grafiği için MPAndroidChart
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    // Kullanıcının konumunu almak için Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.3.0")
    // Hava durumu widget animasyonları için Lottie
    implementation("com.airbnb.android:lottie:6.4.0")
}