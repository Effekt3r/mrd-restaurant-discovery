plugins {
    alias(libs.plugins.mrd.android.library)
    alias(libs.plugins.mrd.hilt)
}

android {
    namespace = "com.effekt3r.mrd.core.network"
}

dependencies {
    implementation(projects.core.common)
}
