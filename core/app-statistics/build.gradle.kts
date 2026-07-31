plugins {
    alias(libs.plugins.apkanalyzer.library)
    alias(libs.plugins.apkanalyzer.hilt)
    alias(libs.plugins.parcelize)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.appstatistics"

    buildFeatures {
        androidResources = false
    }
}

dependencies {
    implementation(projects.core.apps)
    implementation(projects.core.common)
}
