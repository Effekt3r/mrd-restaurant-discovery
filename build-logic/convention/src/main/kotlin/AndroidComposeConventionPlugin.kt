import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.effekt3r.mrd.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/** Compose for an Android application or library module, with the shared stability configuration. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")
            pluginManager.withPlugin("com.android.application") {
                configureCompose(extensions.getByType<ApplicationExtension>())
            }
            pluginManager.withPlugin("com.android.library") {
                configureCompose(extensions.getByType<LibraryExtension>())
            }
            extensions.configure<ComposeCompilerGradlePluginExtension> {
                @Suppress("UnstableApiUsage")
                stabilityConfigurationFiles.add(isolated.rootProject.projectDirectory.file("compose-stability.conf"))
            }
        }
    }

    private fun Project.configureCompose(extension: CommonExtension) {
        extension.buildFeatures.compose = true
        dependencies {
            "implementation"(platform(library("androidx-compose-bom")))
            "testImplementation"(platform(library("androidx-compose-bom")))
            "implementation"(library("androidx-compose-ui-tooling-preview"))
            "debugImplementation"(library("androidx-compose-ui-tooling"))
        }
    }
}
