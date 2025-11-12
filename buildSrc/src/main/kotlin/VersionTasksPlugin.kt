import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.Properties

class VersionTasksPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("setVersionName") {
            notCompatibleWithConfigurationCache("uses Project API directly")
            doLast {
                val propsFile = project.rootProject.file("version.properties")
                val props = Properties().apply { load(propsFile.inputStream()) }

                val versionName = project.findProperty("versionName") as String?
                if (versionName != null) {
                    props["VERSION_NAME"] = versionName
                    propsFile.outputStream().use { props.store(it, null) }
                    println("Updated VERSION_NAME to $versionName")
                } else {
                    println("No versionName property found, skipping.")
                }
            }
        }
        project.tasks.register("bumpVersionCode") {
            notCompatibleWithConfigurationCache("uses Project API directly")
            doLast {
                val propsFile = project.rootProject.file("version.properties")
                val props = Properties().apply { load(propsFile.inputStream()) }

                val current = props["VERSION_CODE"].toString().toInt()
                val newCode = current + 1
                props["VERSION_CODE"] = newCode.toString()
                propsFile.outputStream().use { props.store(it, null) }

                println("Bumped VERSION_CODE from $current to $newCode")
            }
        }

    }
}
