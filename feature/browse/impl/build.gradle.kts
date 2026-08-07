plugins {
    alias(libs.plugins.apkanalyzer.feature.impl)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.browse.impl"

    buildFeatures {
        androidResources = false
    }
}

dependencies {
    api(projects.feature.browse.api)
}
