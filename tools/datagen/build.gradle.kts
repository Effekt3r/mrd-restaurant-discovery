plugins {
    alias(libs.plugins.mrd.jvm.library)
    application
}

// A build-time tool that writes the mock API's data. It never ships in the app.
application {
    mainClass = "com.effekt3r.mrd.tools.datagen.MainKt"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}

// ./gradlew :tools:datagen:run regenerates both datasets; --args=fetch-images refreshes the image pool.
// The tool reads and writes paths relative to the repository root.
tasks.named<JavaExec>("run") {
    workingDir = rootDir
    args("generate")
}

// GoldenFilesTest reads the committed mock-api/v1/ files from disk, which Gradle can't see. Declaring them as an input
// makes Gradle re-run the tests when one changes, instead of reporting UP-TO-DATE or restoring a cached pass. RELATIVE
// keeps the build-cache key the same wherever the repository is checked out.
tasks.test {
    inputs.dir(rootDir.resolve("mock-api/v1"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("mockApiFiles")
}
