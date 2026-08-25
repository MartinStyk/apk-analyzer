plugins {
    alias(libs.plugins.apkanalyzer.library)
    alias(libs.plugins.apkanalyzer.hilt)
    alias(libs.plugins.apkanalyzer.appfunctions)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.appfunctions"

    buildFeatures {
        androidResources = false
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.apps)
}
