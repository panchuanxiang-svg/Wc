import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 定义项目版本和 SDK 常量
val versionCode by extra(93)
val versionName by extra("2.1.2")

val compileSdk by extra(37)
val targetSdk by extra(36)
val minSdk by extra(26)

val groupName by extra("tk.zwander")
val packageName by extra("tk.zwander.samsungfirmwaredownloader")
val appName by extra("Bifrost")

val bugsnagJvmApiKey by extra("a5b9774e86bc615c2e49a572b8313489")
val bugsnagAndroidApiKey by extra("3e0ed592029da1d5cc9b52160ef702ea")

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.bugsnag.gradle) apply false
    alias(libs.plugins.buildkonfig) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.conveyor) apply false
    alias(libs.plugins.kotlin.atomicfu) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.moko.resources) apply false
}

// 统一配置所有子模块，确保 JVM 21 环境和编译器参数生效
subprojects {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            // 使用 JvmTarget 枚举类型，这是 Kotlin 2.0+ 的最佳实践
            jvmTarget.set(JvmTarget.JVM_21)
            
            // 优化编译参数
            freeCompilerArgs.addAll(
                "-Xskip-prerelease-check",
                "-Xdont-warn-on-error-suppression"
            )
        }
    }
}

group = groupName
version = versionName
