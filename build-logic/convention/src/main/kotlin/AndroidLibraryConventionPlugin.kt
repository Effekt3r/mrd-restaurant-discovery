import com.android.build.api.dsl.LibraryExtension
import com.effekt3r.mrd.buildlogic.TARGET_SDK
import com.effekt3r.mrd.buildlogic.configureKotlinAndroid
import com.effekt3r.mrd.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** An Android library module. Unit tests can read Android resources (needed by Robolectric). */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")
            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                testOptions.targetSdk = TARGET_SDK
                lint.targetSdk = TARGET_SDK
                testOptions.unitTests.isIncludeAndroidResources = true
            }
            dependencies {
                "testImplementation"(library("junit"))
            }
        }
    }
}
