plugins {
    alias(libs.plugins.android.application)
    // 👈 移除不匹配的多平台插件，在这个作为外壳的 android 模块中
    // 引入常规的 kotlin.android 插件或与其保持一致
    alias(libs.plugins.kotlin.android) 
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

    // 👈 核心修改：在 KMP 架构的 android 壳模块中，
    // kotlinOptions 必须直接写在 android {} 块内！
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs += listOf(
            "-Xskip-prerelease-check",
            "-Xdont-warn-on-error-suppression"
        )
    }
}
