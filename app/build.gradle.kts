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
        versionCode = 3
        versionName = "2.1-demon"
    }

    // Clé gardée dans le cache GitHub (jamais dans le dépôt) pour que les mises à jour s'installent par-dessus
    val cle = System.getenv("LEXOS_CLE")
    signingConfigs {
        if (cle != null && file(cle).exists()) create("lexos") {
            storeFile = file(cle)
            storePassword = System.getenv("LEXOS_CLE_MDP")
            keyAlias = "lexosdemon"
            keyPassword = System.getenv("LEXOS_CLE_MDP")
        }
    }
    buildTypes {
        getByName("debug") { signingConfigs.findByName("lexos")?.let { signingConfig = it } }
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
