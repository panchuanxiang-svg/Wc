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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {

    // =========================
    // ✅ desugar（Java 8+ API 兼容）
    // =========================
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    // =========================
    // ✅ 依赖 common 模块
    // =========================
    implementation(project(":common"))
}
