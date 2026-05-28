plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android) // 👈 现在 toml 补全后，这里不会再报错了
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.moko.resources)
}

android {
    compileSdk = rootProject.extra["compileSdk"] as Int
    namespace = rootProject.extra["packageName"] as String

    defaultConfig {  
        applicationId = namespace  
        minSdk = rootProject.extra["minSdk"] as Int  
        targetSdk = rootProject.extra["targetSdk"] as Int  
        versionCode = rootProject.extra["versionCode"] as Int  
        versionName = rootProject.extra["versionName"] as String  
    }  

    compileOptions {  
        sourceCompatibility = JavaVersion.VERSION_21  
        targetCompatibility = JavaVersion.VERSION_21  
        isCoreLibraryDesugaringEnabled = true  
    }

    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs += listOf(
            "-Xskip-prerelease-check",
            "-Xdont-warn-on-error-suppression"
        )
    }
}
