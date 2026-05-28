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
        // 开启 Java 8+ API 脱糖，解决旧设备不兼容问题
        isCoreLibraryDesugaringEnabled = true
        
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

// ✅ 关键修复：完全弃用旧的 kotlinOptions，使用标准的 compilerOptions
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    // 必需：脱糖库依赖
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    
    // 你的业务代码依赖
    implementation(project(":common"))
    
    // 其他 Android 相关依赖，例如 compose 等
    // implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    // implementation("androidx.compose.ui:ui")
}
