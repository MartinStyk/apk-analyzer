plugins {
    alias(libs.plugins.apkanalyzer.feature.phone)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.appexport.phone"
}

dependencies {
    api(projects.feature.appExport.api)
    implementation(projects.feature.apps.api)
    implementation(projects.core.appExport)
    implementation(projects.core.appPermissions)
    implementation(projects.core.apps)
    implementation(projects.core.common)
}
