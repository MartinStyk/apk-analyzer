plugins {
    alias(libs.plugins.apkanalyzer.core.impl)
    alias(libs.plugins.apkanalyzer.tests.unit)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.apps.impl"
}

dependencies {
    implementation(projects.core.apps.api)
    implementation(projects.core.common)
    implementation(libs.androidx.core.ktx)
}
