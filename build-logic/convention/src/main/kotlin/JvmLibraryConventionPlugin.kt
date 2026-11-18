import com.effekt3r.mrd.buildlogic.configureKotlinJvm
import com.effekt3r.mrd.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/** A pure Kotlin module: no Android, so its tests run on the plain JVM. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.jvm")
            apply(plugin = "com.android.lint")
            configureKotlinJvm()
            // Android modules run their unit tests with testDebugUnitTest. Giving pure Kotlin modules a task with the same
            // name means ./gradlew testDebugUnitTest runs every module's tests, so CI can't miss a new module.
            tasks.register("testDebugUnitTest") {
                group = "verification"
                description = "Runs this module's unit tests (an alias of test)."
                dependsOn("test")
            }
            dependencies {
                "testImplementation"(library("junit"))
            }
        }
    }
}
