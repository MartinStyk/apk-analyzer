plugins {
    alias(libs.plugins.apkanalyzer.library)
    alias(libs.plugins.apkanalyzer.hilt)
    alias(libs.plugins.apkanalyzer.tests.unit)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.browse.domain"

    androidResources.enable = false
}

dependencies {
    api(projects.feature.browse.api)
    implementation(projects.core.appIndex)
    implementation(projects.core.common)

    testImplementation(testFixtures(projects.core.common))
}
