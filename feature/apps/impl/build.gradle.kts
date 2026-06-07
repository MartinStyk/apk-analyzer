plugins {
    alias(libs.plugins.apkanalyzer.feature.impl)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.apps.impl"
}

dependencies {
    api(projects.feature.apps.api)
    implementation(projects.feature.settings.api)
    implementation(projects.feature.appDetail.api)
    implementation(projects.core.apps)
    implementation(projects.core.userPreferences)
    implementation(projects.core.appPermissions)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.coil.compose)
}
