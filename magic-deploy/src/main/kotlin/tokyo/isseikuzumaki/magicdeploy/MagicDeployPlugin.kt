package tokyo.isseikuzumaki.magicdeploy

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject
import java.io.File

abstract class NotifyApkPathTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Internal
    abstract val apkDirectory: DirectoryProperty
    
    @get:Input
    @get:Optional
    abstract val port: Property<Int>

    @TaskAction
    fun notifyPath() {
        val dir = apkDirectory.get().asFile
        if (!dir.exists()) {
            println("APK directory not found: $dir")
            return
        }
        // Find .apk files, excluding "unaligned" ones
        val apkFile = dir.walkTopDown().find { it.name.endsWith(".apk") && !it.name.contains("unaligned") }

        if (apkFile != null) {
            val absolutePath = apkFile.absolutePath
            println("Found APK at: $absolutePath")
            val targetPort = port.getOrElse(58080)
            
            try {
                // Use 'sh -c' to pipe the path to nc
                execOperations.exec {
                    commandLine("sh", "-c", "echo \"$absolutePath\" | nc -w 1 localhost $targetPort")
                    isIgnoreExitValue = true
                }
                println("Sent APK path to localhost:$targetPort")
            } catch (e: Exception) {
                println("Failed to send APK path: ${e.message}")
            }
        } else {
            println("APK file not found in $dir")
        }
    }
}

class MagicDeployPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val notifyTask = project.tasks.register("notifyApkPath", NotifyApkPathTask::class.java) {
            apkDirectory.set(project.layout.buildDirectory.dir("outputs/apk/debug"))
            port.set(58080)
        }

        project.tasks.configureEach {
            if (name == "assembleDebug") {
                finalizedBy(notifyTask)
                println("Magic Deploy: Hooked into assembleDebug")
            }
        }
    }
}
