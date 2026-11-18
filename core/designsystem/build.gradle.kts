plugins {
    alias(libs.plugins.mrd.android.library)
    alias(libs.plugins.mrd.android.compose)
}

android {
    namespace = "com.effekt3r.mrd.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
}
