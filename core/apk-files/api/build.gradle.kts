plugins {
    alias(libs.plugins.apkanalyzer.core.api)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.apkfiles.api"

    androidResources.enable = false
}

dependencies {
    implementation(projects.core.common)
}
