plugins {
    alias(libs.plugins.mrd.android.library)
}

android {
    namespace = "com.effekt3r.mrd.core.testing"
}

dependencies {
    api(projects.core.data)
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
