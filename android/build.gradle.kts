plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.moko.resources)
}

group = rootProject.extra["groupName"].toString()
version = rootProject.extra["versionName"].toString()

android {
    val compileSdkVal: Int by rootProject.extra
    val packageName: String by rootProject.extra

    compileSdk = compileSdkVal
    namespace = packageName

    defaultConfig {
        applicationId = packageName

        val minSdkVal: Int by rootProject.extra
        val targetSdkVal: Int by rootProject.extra
        val versionCodeVal: Int by rootProject.extra
        val versionNameVal: String by rootProject.extra

        minSdk = minSdkVal
        targetSdk = targetSdkVal
        versionCode = versionCodeVal
        versionName = versionNameVal

        resValue("string", "app_name", rootProject.extra["appName"].toString())
    }

    buildFeatures {
        compose = true
        aidl = true
        resValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    packaging {
        resources.excludes.add("META-INF/versions/9/previous-compilation-data.bin")
    }
}
