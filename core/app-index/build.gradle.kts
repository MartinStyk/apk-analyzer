plugins {
    alias(libs.plugins.apkanalyzer.library)
    alias(libs.plugins.apkanalyzer.hilt)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.appindex"
}

dependencies {
    implementation(projects.core.appPermissions)
    implementation(projects.core.apps.api)
    implementation(projects.core.common)
}
