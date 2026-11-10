plugins {
    alias(libs.plugins.apkanalyzer.feature.phone)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.appdetail.phone"
}

dependencies {
    api(projects.feature.appDetail.api)
    implementation(projects.feature.appDetail.domain)
    implementation(projects.feature.appExport.api)
    implementation(projects.core.apkFiles)
    implementation(projects.core.appHistory)
    implementation(projects.core.apps)
    implementation(projects.core.aiInsights)
    implementation(projects.core.appPermissions)
    implementation(projects.core.common)
    implementation(projects.core.userPreferences)
    implementation(libs.coil.compose)
}
