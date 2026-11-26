plugins {
    alias(libs.plugins.apkanalyzer.core.api)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.aiinsights.api"

    androidResources.enable = false
}

dependencies {
    api(projects.core.common)
}
