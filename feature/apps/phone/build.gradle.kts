plugins {
    alias(libs.plugins.apkanalyzer.feature.phone)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.apps.phone"
}

dependencies {
    api(projects.feature.apps.api)
    implementation(projects.feature.settings.api)
    implementation(projects.feature.appDetail.api)
    implementation(projects.feature.appExport.api)
    implementation(projects.core.apkFiles)
    implementation(projects.core.apps)
    implementation(projects.core.userPreferences)
    implementation(projects.core.appPermissions)
    implementation(libs.coil.compose)
}
