plugins {
    alias(libs.plugins.apkanalyzer.feature.impl)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.permissions.impl"

    buildFeatures {
        androidResources = false
    }
}

dependencies {
    api(projects.feature.permissions.api)
}
