plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.zentora.app.goalshift"
    compileSdk {
        version = release(37)
    }

    val suppliedIcons = file("${System.getProperty("user.home")}/Downloads/icon_LionQuote")
    val launcherResources = if (suppliedIcons.list()?.contains("mipmap-mdpi") == true) {
        suppliedIcons
    } else {
        file("src/main/launcher-res")
    }
    sourceSets.getByName("main").res.directories.add(launcherResources.absolutePath)

    defaultConfig {
        applicationId = "com.zentora.app.goalshift"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}