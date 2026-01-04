import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "VibeTerminal"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared-ui"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)

            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.voyager.navigator)
            implementation(libs.voyager.screenmodel)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.koin)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.datastore)
            implementation(libs.datastore.preferences)

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation("io.github.isseikz:kmp-terminal-input:1.0.3")
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
            implementation(libs.mina.sshd.core)
            implementation(libs.mina.sshd.common)
            implementation(libs.mina.sshd.sftp)
            implementation(libs.room.runtime)
            implementation(libs.room.ktx)
            implementation(libs.timber)
            // Explicit dependencies for ViewTree classes
            implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
            implementation("androidx.lifecycle:lifecycle-process:2.9.4")
            implementation("androidx.savedstate:savedstate:1.2.1")
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.mina.sshd.core)
                implementation(libs.mina.sshd.common)
                implementation(libs.mina.sshd.sftp)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspCommonMainMetadata", libs.room.compiler)
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

android {
    namespace = "tokyo.isseikuzumaki.vibeterminal"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "tokyo.isseikuzumaki.vibeterminal"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        getByName("debug") {
            val keystoreFile = rootProject.file("debug.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
            } else {
                storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            }
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
    debugImplementation("com.willowtreeapps.hyperion:hyperion-core:0.9.38")
    debugImplementation("com.willowtreeapps.hyperion:hyperion-timber:0.9.38")
}

abstract class NotifyApkPathTask : DefaultTask() {
    @get:javax.inject.Inject
    abstract val execOperations: org.gradle.process.ExecOperations
    
    @get:InputDirectory
    abstract val apkDirectory: DirectoryProperty

    @TaskAction
    fun notifyPath() {
        val dir = apkDirectory.get().asFile
        val apkFile = dir.walkTopDown().find { it.name.endsWith(".apk") && !it.name.contains("unaligned") }
        
        if (apkFile != null) {
            val absolutePath = apkFile.absolutePath
            println("Found APK at: $absolutePath")
            try {
                execOperations.exec {
                    commandLine("sh", "-c", "echo \"$absolutePath\" | nc -w 1 localhost 58080")
                    isIgnoreExitValue = true
                }
                println("Sent APK path to localhost:58080")
            } catch (e: Exception) {
                println("Failed to send APK path: ${e.message}")
            }
        } else {
            println("APK file not found in $dir")
        }
    }
}

tasks.register<NotifyApkPathTask>("notifyApkPath") {
    apkDirectory.set(layout.buildDirectory.dir("outputs/apk/debug"))
}

// Hook into assembleDebug
afterEvaluate {
    tasks.named("assembleDebug") {
        finalizedBy("notifyApkPath")
    }
}