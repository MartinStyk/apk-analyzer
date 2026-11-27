plugins {
    alias(libs.plugins.apkanalyzer.core.impl)
    alias(libs.plugins.apkanalyzer.tests.unit)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.featureflags.impl"
    androidResources.enable = false
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(projects.core.featureFlags.api)
}
