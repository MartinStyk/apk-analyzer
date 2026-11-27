plugins {
    alias(libs.plugins.apkanalyzer.feature.phone)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.appexport.phone"
}

dependencies {
    api(projects.feature.appExport.api)
    implementation(projects.feature.apps.api)
    implementation(projects.feature.pro.api)
    implementation(projects.core.appExport.api)
    implementation(projects.core.appPermissions.api)
    implementation(projects.core.apps.api)
    implementation(projects.core.common)
    implementation(projects.core.entitlements.api)
}
