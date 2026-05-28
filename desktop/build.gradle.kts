plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.conveyor)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.hot.reload)
}

group = rootProject.extra["groupName"].toString()
version = rootProject.extra["versionName"].toString()

val javaVersionEnum: JavaVersion by rootProject.extra

kotlin {
    jvmToolchain(21)

    jvm("desktop")

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":common"))
                implementation(libs.vaqua)
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.withType<org.gradle.jvm.tasks.Jar> {
    exclude("META-INF/*.RSA", "META-INF/*.DSA", "META-INF/*.SF")
}
