plugins {
    alias(libs.plugins.apkanalyzer.library)
    alias(libs.plugins.apkanalyzer.hilt)
    alias(libs.plugins.apkanalyzer.room)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.apphistory"

    androidResources.enable = false
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.apps)
    implementation(libs.kotlinx.serialization.json)
}
