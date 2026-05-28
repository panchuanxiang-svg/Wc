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
    // 这里的内部已经完全清空了旧的 kotlinOptions，防止 Android 插件版本过低导致不认识新语法
}

// 🟢 终极兼容方案：直接作用于全局 Kotlin 编译任务，100% 解决 jvmTarget 报错
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        // 1. 严格锁定 JVM 21 目标版本
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        
        // 2. 注入编译参数（使用标准的 listOf 传递，规避 Gradle 类型的 DSL 报错）
        freeCompilerArgs.addAll(listOf("-Xskip-prerelease-check", "-Xdont-warn-on-error-suppression"))
    }
}
