plugins {
    alias(libs.plugins.apkanalyzer.core.impl)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.entitlements.noop"

    androidResources.enable = false
}

dependencies {
    implementation(projects.core.entitlements.api)
}
