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
    implementation(projects.core.apkFiles.api)
    implementation(projects.core.apps.api)
    implementation(projects.core.userPreferences.api)
    implementation(projects.core.appPermissions.api)
    implementation(libs.coil.compose)
}
