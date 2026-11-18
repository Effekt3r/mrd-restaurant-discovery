package com.effekt3r.mrd.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

internal const val COMPILE_SDK = 36
internal const val TARGET_SDK = 36

// 26 gives java.time without core library desugaring.
internal const val MIN_SDK = 26

/** SDK levels and Java/Kotlin targets shared by every Android module. AGP 9 provides Kotlin built in. */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = COMPILE_SDK
        defaultConfig.minSdk = MIN_SDK
        compileOptions.sourceCompatibility = JavaVersion.VERSION_17
        compileOptions.targetCompatibility = JavaVersion.VERSION_17
    }
    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
    // Hilt's bytecode transform copies the generated test R classes into the unit-test classes, so a module
    // without unit tests would fail Gradle 9's "no tests discovered" check. Keep the check where tests exist.
    val hasUnitTests = layout.projectDirectory.dir("src/test").asFile.exists()
    tasks.withType<Test>().configureEach { failOnNoDiscoveredTests.set(hasUnitTests) }
}

/** Java/Kotlin targets for pure Kotlin modules. */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    extensions.configure<KotlinJvmProjectExtension> {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        // Compile against the JDK 17 API, not the JDK running Gradle: these modules run on Android, where
        // JDK 21-only members such as List.removeFirst crash below API 35.
        compilerOptions.freeCompilerArgs.add("-Xjdk-release=17")
    }
    tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
}
