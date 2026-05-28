pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()

        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/")
        maven("https://maven.hq.hydraulic.software")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    // ✅ 已修复：将 FAIL_ON_PROJECT_REPOS 改为 PREFER_SETTINGS
    // 允许子模块（如 :android）声明自己的独占仓库，防止 Android 插件解析链直接崩溃断开
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) 
    
    repositories {
        mavenCentral()
        google()

        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/")
        maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap/")
        maven("https://jitpack.io")
        maven("https://repo.jenkins-ci.org/public/")
    }
}

rootProject.name = "SamloaderKotlin"
include(":android")
include(":desktop")
include(":common")
