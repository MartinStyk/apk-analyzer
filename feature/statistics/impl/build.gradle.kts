plugins {
    alias(libs.plugins.apkanalyzer.feature.impl)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.statistics.impl"

    buildFeatures {
        androidResources = false
    }
}

dependencies {
    api(projects.feature.statistics.api)
}
