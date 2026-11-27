plugins {
    alias(libs.plugins.apkanalyzer.feature.phone)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.whatchanged.phone"
}

dependencies {
    api(projects.feature.whatChanged.api)
    implementation(projects.feature.appDetail.api)
    implementation(projects.feature.pro.api)
    implementation(projects.core.appHistory.api)
    implementation(projects.core.apps.api)
    implementation(projects.core.appPermissions.api)
    implementation(projects.core.common)
    implementation(projects.core.entitlements.api)
}
