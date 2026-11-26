plugins {
    alias(libs.plugins.apkanalyzer.core.api)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.apps.api"
}

dependencies {
    api(projects.core.common)
    implementation(libs.androidx.core.ktx)
}
