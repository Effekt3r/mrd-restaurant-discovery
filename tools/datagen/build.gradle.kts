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
