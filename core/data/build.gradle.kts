plugins {
    alias(libs.plugins.mrd.android.library)
    alias(libs.plugins.mrd.hilt)
}

android {
    namespace = "com.effekt3r.mrd.core.data"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.network)
}
