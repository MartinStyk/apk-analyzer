plugins {
    alias(libs.plugins.apkanalyzer.core.impl)
    alias(libs.plugins.apkanalyzer.tests.unit)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.featureflags.debug"
    androidResources.enable = false
}

dependencies {
    implementation(projects.core.featureFlags.api)
    implementation(projects.core.common)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(testFixtures(projects.core.featureFlags.api))
    testImplementation(testFixtures(projects.core.common))
}
