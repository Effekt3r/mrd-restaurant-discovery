plugins {
    alias(libs.plugins.mrd.android.library)
    alias(libs.plugins.mrd.android.compose)
}

android {
    namespace = "com.effekt3r.mrd.core.ui"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)
}
