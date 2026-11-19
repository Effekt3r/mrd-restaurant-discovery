plugins {
    alias(libs.plugins.mrd.jvm.library)
    alias(libs.plugins.mrd.hilt)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
