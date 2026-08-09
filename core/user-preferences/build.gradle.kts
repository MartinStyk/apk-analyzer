plugins {
    alias(libs.plugins.apkanalyzer.library)
    alias(libs.plugins.apkanalyzer.hilt)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.userpreferences"

    androidResources.enable = false
}

dependencies {
    implementation(projects.core.apps)
    implementation(projects.core.common)
}
