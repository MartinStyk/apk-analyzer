plugins {
    alias(libs.plugins.apkanalyzer.feature.phone)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.appdetail.phone"
}

dependencies {
    implementation(projects.core.featureFlags.api)
    api(projects.feature.appDetail.api)
    implementation(projects.feature.appDetail.domain)
    implementation(projects.feature.appExport.api)
    implementation(projects.core.apkFiles.api)
    implementation(projects.core.appHistory.api)
    implementation(projects.core.apps.api)
    implementation(projects.core.aiInsights.api)
    implementation(projects.core.appPermissions.api)
    implementation(projects.core.common)
    implementation(projects.core.userPreferences.api)
    implementation(libs.coil.compose)
}
