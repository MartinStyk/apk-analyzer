plugins {
    alias(libs.plugins.apkanalyzer.library)
    alias(libs.plugins.apkanalyzer.compose)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.navigation"

    androidResources.enable = false
}

dependencies {
    implementation(libs.kotlinx.collections.immutable)
}
