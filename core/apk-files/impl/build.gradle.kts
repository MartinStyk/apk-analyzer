plugins {
    alias(libs.plugins.apkanalyzer.core.impl)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.apkfiles.impl"

    androidResources.enable = false
}

dependencies {
    implementation(projects.core.apkFiles.api)
    implementation(projects.core.common)
}
