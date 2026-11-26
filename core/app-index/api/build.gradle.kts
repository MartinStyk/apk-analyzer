plugins {
    alias(libs.plugins.apkanalyzer.core.api)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.appindex.api"
}

dependencies {
    api(projects.core.apps.api)
    implementation(projects.core.common)
    implementation(libs.androidx.core.ktx)
}
