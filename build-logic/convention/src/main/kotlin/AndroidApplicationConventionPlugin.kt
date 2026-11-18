import com.android.build.api.dsl.ApplicationExtension
import com.effekt3r.mrd.buildlogic.TARGET_SDK
import com.effekt3r.mrd.buildlogic.configureKotlinAndroid
import com.effekt3r.mrd.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** The application module: SDK levels plus the lint policy for the whole app (it checks dependencies too). */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")
            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = TARGET_SDK
                lint {
                    checkDependencies = true
                    abortOnError = true
                    // Versions are pinned on purpose (README: toolchain and version policy).
                    // Dependabot proposes updates once a release is 30 days old.
                    disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
                }
            }
            dependencies {
                "testImplementation"(libs.findLibrary("junit").get())
            }
        }
    }
}
