plugins {
    alias(libs.plugins.apkanalyzer.core.api)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.userpreferences.api"

    androidResources.enable = false
}

dependencies {
    api(projects.core.apps.api)
}
