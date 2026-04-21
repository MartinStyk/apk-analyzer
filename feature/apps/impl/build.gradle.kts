plugins {
    alias(libs.plugins.apkanalyzer.feature.impl)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.apps.impl"
}

dependencies {
    api(projects.feature.apps.api)
    implementation(projects.core.apps)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.coil.compose)
}
