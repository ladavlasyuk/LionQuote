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
        minSdk = 25
        targetSdk = 36
        versionCode = 21
        versionName = "2.1"
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity)
}
