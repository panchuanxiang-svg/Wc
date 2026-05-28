plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.compose")
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

    iosArm64()
    iosSimulatorArm64()

    sourceSets {

        val commonMain by getting {
            dependencies {

                // Compose
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)

                // Kotlin
                implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.21")

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

                // Datetime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

                // Ktor
                implementation("io.ktor:ktor-client-core:2.3.12")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
                implementation("io.ktor:ktor-client-auth:2.3.12")

                // Ksoup
                implementation("com.mohamedrejeb.ksoup:ksoup:0.2.1")
            }
        }

        val androidMain by getting {
            dependencies {

                // Android Ktor Engine
                implementation("io.ktor:ktor-client-okhttp:2.3.12")
            }
        }

        val desktopMain by getting {
            dependencies {

                // Desktop Ktor Engine
                implementation("io.ktor:ktor-client-cio:2.3.12")
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)

            iosArm64Main.dependsOn(iosMain)
            iosSimulatorArm64Main.dependsOn(iosMain)

            dependencies {

                // iOS Ktor Engine
                implementation("io.ktor:ktor-client-darwin:2.3.12")
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
