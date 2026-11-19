plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.spotless)
}

// Formatting for every Kotlin and Gradle Kotlin file (ktlint; style settings in .editorconfig).
// Run ./gradlew spotlessApply before committing; CI runs spotlessCheck.
spotless {
    // A file tree, not a "**/" string: Spotless then doesn't read every subproject's build directory
    // (an isolated-projects violation), and hidden top-level folders (.gradle, .worktrees, .claude) are skipped.
    fun sources(pattern: String) = fileTree(rootDir) {
        include(pattern)
        exclude("**/build/**", ".*/**")
    }
    kotlin {
        target(sources("**/*.kt"))
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target(sources("**/*.kts"))
        ktlint(libs.versions.ktlint.get())
    }
}
