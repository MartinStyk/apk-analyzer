plugins {
    alias(libs.plugins.apkanalyzer.core.api)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.appexport.api"

    androidResources.enable = false
}

dependencies {
    api(projects.core.apps.api)
}
