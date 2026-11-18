import com.effekt3r.mrd.buildlogic.configureKotlinJvm
import com.effekt3r.mrd.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/** A pure Kotlin module: no Android, so its tests run on the plain JVM. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.jvm")
            configureKotlinJvm()
            dependencies {
                "testImplementation"(libs.findLibrary("junit").get())
            }
        }
    }
}
