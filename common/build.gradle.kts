plugins { // ✅ 已修复：首字母必须小写
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.compose")
}

kotlin {
    // ✅ 已修复：适配 Kotlin 2.0+ 规范，配置多平台共享模块编译 Android 库时的 target 级别
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    // ✅ 已修复：同步配置 JVM (Desktop) 端的编译器目标级别为 21，保证双端一致
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {

        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)

                // ⚠️ 注意：kotlin-reflect 是 JVM 独占库，原先放在这里会导致 iOS 编译直接报找不到依赖而崩溃。
                // 已将其安全移动到下方的 androidMain 和 desktopMain 中。
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

                implementation("io.ktor:ktor-client-core:2.3.12")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
                implementation("io.ktor:ktor-client-auth:2.3.12")

                implementation("com.mohamedrejeb.ksoup:ksoup:0.2.1")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:2.3.12")
                implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.21") // ✅ 安全移动至此
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio:2.3.12")
                implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.21") // ✅ 安全移动至此
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)

            dependencies {
                implementation("io.ktor:ktor-client-darwin:2.3.12")
            }
        }
        
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
    }
}

android {
    namespace = "tk.zwander.common"

    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
