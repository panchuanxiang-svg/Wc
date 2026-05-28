plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget()
    jvm {
        compilations.all {
            compilerOptions.configure {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            }
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            // 修复：更新至最新维护版本，解决依赖解析报错
            implementation("com.fleeksoft.ksoup:ksoup:0.2.6")
        }
    }
}

android {
    namespace = "tk.zwander.common"
    compileSdk = 34
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
