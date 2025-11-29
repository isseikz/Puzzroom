plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Add common dependencies if needed
        }
        androidMain.dependencies {
            implementation("androidx.core:core-ktx:1.12.0")
        }
    }
}

android {
    namespace = "com.puzzroom.whisper"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86"))
        }

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17")
                // Enable NNAPI for hardware acceleration
                arguments("-DWHISPER_NNAPI=ON")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/androidMain/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
    }
}
