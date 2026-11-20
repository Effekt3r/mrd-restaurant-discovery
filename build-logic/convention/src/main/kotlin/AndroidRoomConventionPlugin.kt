import androidx.room.gradle.RoomExtension
import com.effekt3r.mrd.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Room with KSP. The schema of every database version is exported to the module's schemas/ folder and committed, so
 * later migrations can be tested against it.
 */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "androidx.room")
            apply(plugin = "com.google.devtools.ksp")
            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }
            dependencies {
                "implementation"(library("androidx-room-runtime"))
                "ksp"(library("androidx-room-compiler"))
            }
        }
    }
}
