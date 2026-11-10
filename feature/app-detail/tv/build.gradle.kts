plugins {
    alias(libs.plugins.apkanalyzer.feature.tv)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.appdetail.tv"
}

dependencies {
    api(projects.feature.appDetail.api)
    implementation(projects.feature.appDetail.domain)
    implementation(projects.core.appPermissions)
    implementation(projects.core.apps)
    implementation(projects.core.common)
    implementation(projects.core.userPreferences)
}
