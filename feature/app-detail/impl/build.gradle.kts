plugins {
    alias(libs.plugins.apkanalyzer.feature.impl)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.appdetail.impl"
}

dependencies {
    api(projects.feature.appDetail.api)
    implementation(projects.core.apkFiles)
    implementation(projects.core.apps)
    implementation(projects.core.aiInsights)
    implementation(projects.core.appPermissions)
    implementation(projects.core.common)
    implementation(projects.core.userPreferences)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.coil.compose)
}
