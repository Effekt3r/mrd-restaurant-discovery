import com.android.build.api.dsl.LibraryExtension
import com.effekt3r.mrd.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * A feature module: an Android library with Compose, Hilt, type-safe navigation routes and the shared
 * design system. Features never depend on each other; :app connects them through callbacks (enforced for
 * every project in settings.gradle.kts).
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "mrd.android.library")
            apply(plugin = "mrd.android.compose")
            apply(plugin = "mrd.hilt")
            apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
            extensions.configure<LibraryExtension> {
                // Resource names from every module merge into one namespace; lint fails names without this prefix.
                resourcePrefix = "${name}_"
            }
            dependencies {
                "implementation"(project(":core:designsystem"))
                "implementation"(project(":core:ui"))
                "implementation"(library("androidx-lifecycle-runtime-compose"))
                "implementation"(library("androidx-lifecycle-viewmodel-compose"))
                "implementation"(library("androidx-hilt-lifecycle-viewmodel-compose"))
                "implementation"(library("androidx-navigation-compose"))
                "implementation"(library("kotlinx-serialization-core"))
            }
        }
    }
}
