plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "tk.zwander.android"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        // 开启脱糖以兼容较新 Java API
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

// 修复：使用强类型枚举，彻底解决 jvmTarget: String 报错
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    // 脱糖库依赖，配合上面的 isCoreLibraryDesugaringEnabled
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    
    // 依赖 common 模块
    implementation(project(":common"))
}
