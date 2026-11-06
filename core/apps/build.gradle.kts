plugins {
    alias(libs.plugins.apkanalyzer.library)
    alias(libs.plugins.apkanalyzer.hilt)
    alias(libs.plugins.apkanalyzer.tests.unit)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.apps"
}

dependencies {
    api(projects.core.common)
    implementation(libs.androidx.core.ktx)
}
