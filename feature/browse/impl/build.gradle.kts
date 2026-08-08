plugins {
    alias(libs.plugins.apkanalyzer.feature.impl)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.browse.impl"
}

dependencies {
    api(projects.feature.browse.api)
    implementation(projects.feature.appDetail.api)
    implementation(projects.core.appIndex)
    implementation(projects.core.apps)
    implementation(projects.core.appPermissions)
    implementation(projects.core.common)
    implementation(libs.kotlinx.collections.immutable)
}
