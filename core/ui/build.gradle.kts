plugins {
    alias(libs.plugins.mrd.android.library)
    alias(libs.plugins.mrd.android.compose)
}

android {
    namespace = "com.effekt3r.mrd.core.ui"
    // Resource names from every module merge into one namespace; lint fails names without this prefix.
    resourcePrefix = "ui_"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)
    implementation(libs.coil.compose)

    // The formatters read string resources, and the card is tested with Compose on Robolectric.
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
