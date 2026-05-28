plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
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

    // 🟢 核心修改：将旧的 kotlinOptions 改为满足 Kotlin 2.0+ 规范的 compilerOptions
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.addAll("-Xskip-prerelease-check", "-Xdont-warn-on-error-suppression")
    }
}
