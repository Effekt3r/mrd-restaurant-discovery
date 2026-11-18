import com.effekt3r.mrd.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * Hilt with KSP, in any module type and in any plugin order: Android modules get the Hilt Android plugin,
 * pure Kotlin modules get hilt-core. kotlin-metadata-jvm lets Hilt read current Kotlin metadata.
 */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin("com.android.base") {
                apply(plugin = "com.google.devtools.ksp")
                apply(plugin = "com.google.dagger.hilt.android")
                dependencies {
                    "implementation"(library("hilt-android"))
                    "ksp"(library("hilt-compiler"))
                    "ksp"(library("kotlin-metadata"))
                }
            }
            pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                apply(plugin = "com.google.devtools.ksp")
                dependencies {
                    "implementation"(library("hilt-core"))
                    "ksp"(library("hilt-compiler"))
                    "ksp"(library("kotlin-metadata"))
                }
            }
        }
    }
}
