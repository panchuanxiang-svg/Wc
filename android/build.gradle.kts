plugins {
    id("com.android.library") // 或者如果你是主App项目，这里可能是 "com.android.application"
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "tk.zwander.android" // 请检查并确认这和你原本的 namespace 一致
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        // targetSdk 如果有定义可以保留
    }

    compileOptions {
        // ✅ 关键点：开启脱糖功能
        isCoreLibraryDesugaringEnabled = true
        
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    // ✅ 关键点：添加脱糖核心库依赖（这是修复报错的唯一方法）
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    
    // 如果你的 Android 模块依赖 common 模块，请保留下面这行
    implementation(project(":common"))
    
    // 其他你原本的依赖项...
}
