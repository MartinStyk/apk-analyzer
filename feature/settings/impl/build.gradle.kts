plugins {
    alias(libs.plugins.apkanalyzer.feature.impl)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.settings.impl"
}

dependencies {
    api(projects.feature.settings.api)
    implementation(projects.core.common)
}
