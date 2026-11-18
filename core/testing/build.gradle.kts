plugins {
    alias(libs.plugins.mrd.android.library)
}

android {
    namespace = "com.effekt3r.mrd.core.testing"
}

dependencies {
    api(projects.core.model)
    api(libs.junit)
}
