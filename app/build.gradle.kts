plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ca.lexos.demon"
    compileSdk = 34

    defaultConfig {
        applicationId = "ca.lexos.demon"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "2.0-demon"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.webkit:webkit:1.11.0")
}
