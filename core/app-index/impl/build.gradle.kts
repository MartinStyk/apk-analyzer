plugins {
    alias(libs.plugins.apkanalyzer.core.impl)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.appindex.impl"
}

dependencies {
    implementation(projects.core.appIndex.api)
    implementation(projects.core.appPermissions.api)
    implementation(projects.core.apps.api)
    implementation(projects.core.common)
}
