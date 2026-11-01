plugins {
    alias(libs.plugins.apkanalyzer.feature.phone)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.inventoryexport.phone"
}

dependencies {
    api(projects.feature.inventoryExport.api)
    implementation(projects.feature.apps.api)
    implementation(projects.feature.appDetail.api)
    implementation(projects.core.appExport)
    implementation(projects.core.apps)
    implementation(projects.core.appPermissions)
    implementation(projects.core.common)
}
