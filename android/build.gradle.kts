plugins {
    id("com.android.library") // 如果你是在打包 App，这里改成 "com.android.application"
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "tk.zwander.android" // 请保持和你原来一致
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        // 开启脱糖，适配旧版本 Android 的 Java 新 API
        isCoreLibraryDesugaringEnabled = true
        
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    // ✅ 修复：使用 JvmTarget 枚举类，避开 String 类型的校验错误
    kotlinOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21.target
    }
}

dependencies {
    // ✅ 修复：必须添加此依赖，否则会导致编译时找不到脱糖库
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    
    // 如果你有其他依赖，请写在这里
    // implementation(project(":common"))
}
