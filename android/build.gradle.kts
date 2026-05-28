plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android) // 👈 使用了你 toml 里的定义的 kotlin-android 插件
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

    // 👈 完美迁移：确保打包和编译时使用 Java 21 字节码
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs += listOf(
            "-Xskip-prerelease-check",
            "-Xdont-warn-on-error-suppression"
        )
    }
}
