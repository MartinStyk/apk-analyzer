plugins {
    alias(libs.plugins.apkanalyzer.library)
    alias(libs.plugins.apkanalyzer.hilt)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.apkfiles"

    buildFeatures {
        androidResources = false
    }
}

dependencies {
    implementation(projects.core.common)
}
