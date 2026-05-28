plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    alias(libs.plugins.moko.resources)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    jvm("desktop")

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "common"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.compose.foundation)
                api(libs.compose.material3)
                api(libs.compose.runtime)
                api(libs.compose.ui)
                api(libs.material.icons.core)
                api(libs.kotlin)
                api(libs.kotlin.reflect)
                api(libs.kotlinx.coroutines)
                api(libs.kotlinx.datetime)
                api(libs.kotlinx.io.core)
                api(libs.kotlinx.serialization.json)
                api(libs.ksoup)

                // ✅ KTOR 跨平台核心依赖（已彻底移除 cio，让各平台自动适配）
                api(libs.ktor.client.core)
                api(libs.ktor.client.content.negotiation)
                api(libs.ktor.serialization.kotlinx.json)
                api(libs.ktor.client.auth)

                api(libs.moko.resources)
                api(libs.moko.resources.compose)
                api(libs.multiplatformSettings)
                api(libs.multiplatformSettings.noArg)
                api(libs.richeditor.compose)
                api(libs.semver)
                api(libs.filekit.core)
                api(libs.filekit.dialogs.compose)
                api(libs.kmpfile)
                api(libs.kmpplatform)
                api(libs.zwander.composedialog)
                api(libs.zwander.materialyou)
                api(libs.csv)
                api(libs.cryptography.core)
                api(libs.kotlinx.crypto.crc32)
                api(libs.kotlinx.atomicfu)
                api(libs.androidx.performance.annotation)
                api(libs.xmlbuilder)
                api(libs.ketch.core)
                api(libs.ketch.ktor)
                api(libs.ketch.sqlite)
            }
        }

        val androidMain by getting {
            dependencies {
                // 如果你需要特定给 Android 端用的 Ktor 引擎，可以加上这一行：
                // implementation("io.ktor:ktor-client-okhttp:2.3.12")
            }
        }

        val desktopMain by getting {
            dependencies {
                // 如果你需要特定给 Desktop 端用的 Ktor 引擎，可以加上这一行：
                // implementation("io.ktor:ktor-client-cio:2.3.12")
            }
        }
    }
}

android {
    namespace = "tk.zwander.common"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// 如果你用了 buildkonfig 插件，这里可以保留你的配置，没有的话保持默认即可
buildkonfig {
    packageName = "tk.zwander.common"
    defaultConfigs {
        // 可以在这里放你的配置项
    }
}
