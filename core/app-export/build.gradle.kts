plugins {
    alias(libs.plugins.apkanalyzer.library)
    alias(libs.plugins.apkanalyzer.hilt)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.appexport"

    androidResources.enable = false
}

dependencies {
    api(projects.core.apps)
    implementation(projects.core.common)
}
