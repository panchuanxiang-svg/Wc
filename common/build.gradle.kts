plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    androidTarget()
    jvmToolchain(21)

    sourceSets {

        val commonMain by getting {
            dependencies {

                // =====================
                // Compose Multiplatform
                // =====================
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
                implementation(compose.material)
                implementation(compose.material3)
                implementation(compose.animation)

                // =====================
                // Coroutines（修复 launch / scope）
                // =====================
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

                // =====================
                // HTML parser
                // =====================
                implementation("com.fleeksoft.ksoup:ksoup:0.2.6")
            }
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
