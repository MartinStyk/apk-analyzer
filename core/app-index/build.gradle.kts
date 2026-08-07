plugins {
    alias(libs.plugins.apkanalyzer.library)
    alias(libs.plugins.apkanalyzer.hilt)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.appindex"
}

dependencies {
    implementation(projects.core.apps)
    implementation(projects.core.common)
}
