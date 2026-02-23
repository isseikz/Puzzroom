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
import java.io.File
import java.io.PrintWriter
import java.net.Socket

abstract class NotifyApkPathTask : DefaultTask() {

    @get:Internal
    abstract val apkDirectory: DirectoryProperty
    
    @get:Input
    @get:Optional
    abstract val port: Property<Int>

    @TaskAction
    fun notifyPath() {
        val dir = apkDirectory.get().asFile
        if (!dir.exists()) {
            println("Magic Deploy: APK directory not found: $dir")
            return
        }
        // Find .apk files, excluding "unaligned" ones
        val apkFile = dir.walkTopDown().find { it.name.endsWith(".apk") && !it.name.contains("unaligned") }

        if (apkFile != null) {
            val absolutePath = apkFile.absolutePath
            println("Magic Deploy: Found APK at: $absolutePath")
            val targetPort = port.getOrElse(58080)
            
            try {
                Socket("localhost", targetPort).use { socket ->
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    writer.println(absolutePath)
                }
                println("Magic Deploy: Sent APK path to localhost:$targetPort")
            } catch (e: Exception) {
                // Connection refused is expected if no listener is running
                println("Magic Deploy: Failed to send APK path (Is the listener running?): ${e.message}")
            }
        } else {
            println("Magic Deploy: APK file not found in $dir")
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