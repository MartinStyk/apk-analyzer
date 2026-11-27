plugins {
    alias(libs.plugins.apkanalyzer.feature.phone)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.settings.phone"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(projects.feature.settings.api)
    implementation(projects.core.common)
    implementation(projects.core.featureFlags.api)
}
