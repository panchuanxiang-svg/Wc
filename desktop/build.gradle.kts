plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    // 配置 Desktop JVM target
    jvm("desktop") {
        compilerOptions {
            // 使用全路径引用，确保编译期能正确找到 JvmTarget 类
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    sourceSets {
        // Desktop 专有依赖
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("io.ktor:ktor-client-cio:2.3.12")
                implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.21")
            }
        }
    }
}
