plugins {
    alias(libs.plugins.apkanalyzer.library)
    alias(libs.plugins.apkanalyzer.hilt)
    alias(libs.plugins.apkanalyzer.tests.unit)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.appdetail.domain"
}

dependencies {
    api(projects.feature.appDetail.api)
    api(projects.core.apps)
    implementation(projects.core.appHistory)
    implementation(projects.core.common)

    testImplementation(testFixtures(projects.core.apps))
}
