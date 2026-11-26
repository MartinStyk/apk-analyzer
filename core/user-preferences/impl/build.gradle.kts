plugins {
    alias(libs.plugins.apkanalyzer.core.impl)
    alias(libs.plugins.apkanalyzer.room)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.userpreferences.impl"

    androidResources.enable = false
}

dependencies {
    implementation(projects.core.userPreferences.api)
    implementation(projects.core.apps.api)
    implementation(projects.core.common)
}
