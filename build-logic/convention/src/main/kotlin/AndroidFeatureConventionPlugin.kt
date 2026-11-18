import com.effekt3r.mrd.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.withType

/**
 * A feature module: an Android library with Compose, Hilt, type-safe navigation routes and the shared
 * design system. Features never depend on each other; :app connects them through callbacks.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "mrd.android.library")
            apply(plugin = "mrd.android.compose")
            apply(plugin = "mrd.hilt")
            apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
            dependencies {
                "implementation"(project(":core:designsystem"))
                "implementation"(project(":core:ui"))
                "implementation"(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                "implementation"(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                "implementation"(libs.findLibrary("androidx-hilt-lifecycle-viewmodel-compose").get())
                "implementation"(libs.findLibrary("androidx-navigation-compose").get())
                "implementation"(libs.findLibrary("kotlinx-serialization-core").get())
            }
            enforceFeatureIsolation()
        }
    }

    // Fails the build as soon as a feature declares a dependency on another feature. AGP adds a
    // dependency from a module's test configurations back to the module itself, so that one is allowed.
    private fun Project.enforceFeatureIsolation() {
        val self = path
        configurations.configureEach {
            dependencies.withType<ProjectDependency>().configureEach {
                check(path == self || !path.startsWith(":feature:")) {
                    "$self must not depend on $path. Feature modules are independent; connect them in :app."
                }
            }
        }
    }
}
