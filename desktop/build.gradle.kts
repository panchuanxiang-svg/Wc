import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.conveyor)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.hot.reload)
}

group = rootProject.extra["groupName"].toString()
version = rootProject.extra["versionName"].toString()

kotlin {
    jvm()

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":common"))
                implementation(libs.vaqua)
            }
        }
    }
}
