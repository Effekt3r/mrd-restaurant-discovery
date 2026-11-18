import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.effekt3r.mrd.buildlogic.libs
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
            val bom = libs.findLibrary("androidx-compose-bom").get()
            "implementation"(platform(bom))
            "testImplementation"(platform(bom))
            "implementation"(libs.findLibrary("androidx-compose-ui-tooling-preview").get())
            "debugImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
        }
    }
}
