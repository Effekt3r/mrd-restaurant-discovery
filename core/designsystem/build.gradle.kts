plugins {
    alias(libs.plugins.mrd.android.library)
    alias(libs.plugins.mrd.android.compose)
}

android {
    namespace = "com.effekt3r.mrd.core.designsystem"
    // Resource names from every module merge into one namespace; lint fails names without this prefix.
    resourcePrefix = "designsystem_"
}

dependencies {
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)

    // Compose UI tests on Robolectric; ui-test-manifest declares the activity they run in.
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
