plugins {
    alias(libs.plugins.mrd.android.application)
    alias(libs.plugins.mrd.android.compose)
    alias(libs.plugins.mrd.hilt)
}

// Release signing comes from Gradle properties or environment variables, never from the repository.
// Locally, a release build without them falls back to the debug key (with a warning) so anyone can run
// an R8 build. CI release builds pass -Pmrd.requireReleaseSigning=true and fail instead.
fun signingValue(name: String): String? =
    providers.gradleProperty(name).orElse(providers.environmentVariable(name)).orNull

val releaseKeystore = signingValue("MRD_KEYSTORE_FILE")
val requireReleaseSigning = providers.gradleProperty("mrd.requireReleaseSigning").orNull == "true"

android {
    namespace = "com.effekt3r.mrd"

    defaultConfig {
        applicationId = "com.effekt3r.mrd"
        versionName = providers.gradleProperty("mrd.versionName").get()
        versionCode = providers.gradleProperty("mrd.versionCode").get().toInt()
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = signingValue("MRD_KEYSTORE_PASSWORD")
                keyAlias = signingValue("MRD_KEY_ALIAS")
                keyPassword = signingValue("MRD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = when {
                releaseKeystore != null -> signingConfigs.getByName("release")

                requireReleaseSigning -> throw GradleException(
                    "Release signing is required, but MRD_KEYSTORE_FILE is not set."
                )

                else -> signingConfigs.getByName("debug")
            }
        }
    }
}

if (releaseKeystore == null) {
    // Warn when a release APK is actually packaged, not on every build that configures the project.
    tasks.named { it == "packageRelease" }.configureEach {
        doFirst { logger.warn("MRD_KEYSTORE_FILE is not set, so this release APK is signed with the debug key.") }
    }
}

dependencies {
    // The features bring the data layer too; depending on it here keeps its Hilt modules in the app's graph and its
    // code under :app:lintDebug, which lints the app's dependencies, whichever feature uses it.
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    // MrdApplication hands Coil the image loader that :core:network provides.
    implementation(projects.core.network)
    implementation(projects.feature.browse)
    implementation(projects.feature.details)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil)
    testImplementation(libs.androidx.navigation.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.konsist)
    testImplementation(libs.robolectric)
}

// ArchitectureTest (Konsist) reads module sources straight from disk, which Gradle can't see. Every Kotlin source in
// the repository is declared as an input (a superset of what the test reads, so the two can't drift); otherwise
// Gradle reports UP-TO-DATE (or restores a cached pass) after a change to a module that isn't on :app's classpath.
// Build output and hidden top-level folders (.gradle, .worktrees, .claude) are left out, as in Spotless.
tasks.withType<Test>().configureEach {
    inputs.files(
        fileTree(rootDir) {
            include("**/*.kt")
            exclude("**/build/**", ".*/**")
        }
    ).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("architectureSources")
}
