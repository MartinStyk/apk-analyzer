plugins {
    alias(libs.plugins.apkanalyzer.core.impl)
    alias(libs.plugins.apkanalyzer.room)
    alias(libs.plugins.apkanalyzer.work)
    alias(libs.plugins.apkanalyzer.tests.unit)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.apphistory.impl"

    androidResources.enable = false
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(projects.core.appHistory.api)
    implementation(projects.core.apps.api)
    implementation(projects.core.common)
    implementation(libs.kotlinx.serialization.json)

    testFixturesImplementation(projects.core.apps.api)
    testFixturesImplementation(projects.core.common)

    testImplementation(testFixtures(projects.core.apps.api))
    testImplementation(testFixtures(projects.core.common))
}
