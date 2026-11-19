plugins {
    alias(libs.plugins.mrd.android.library)
    alias(libs.plugins.mrd.hilt)
}

android {
    namespace = "com.effekt3r.mrd.core.network"
}

dependencies {
    implementation(projects.core.common)
    // The types this module provides through Hilt.
    api(libs.kotlinx.serialization.json)
    api(libs.okhttp)
}
