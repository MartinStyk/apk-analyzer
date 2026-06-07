plugins {
    alias(libs.plugins.apkanalyzer.feature.impl)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.appdetail.impl"
}

dependencies {
    api(projects.feature.appDetail.api)
    implementation(projects.core.apps)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.coil.compose)
}
