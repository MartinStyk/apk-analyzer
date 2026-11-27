plugins {
    alias(libs.plugins.apkanalyzer.core.impl)
    alias(libs.plugins.apkanalyzer.tests.unit)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.entitlements.impl"

    androidResources.enable = false
}

dependencies {
    implementation(projects.core.entitlements.api)
    implementation(projects.core.common)
    implementation(libs.play.billing.ktx)

    testImplementation(testFixtures(projects.core.common))
}
