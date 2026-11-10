plugins {
    alias(libs.plugins.apkanalyzer.library)
    alias(libs.plugins.apkanalyzer.hilt)
    alias(libs.plugins.apkanalyzer.room)
    alias(libs.plugins.apkanalyzer.work)
    alias(libs.plugins.apkanalyzer.tests.unit)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.apphistory"
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.appPermissions)
    api(projects.core.apps)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(testFixtures(projects.core.apps))
    testImplementation(testFixtures(projects.core.common))
}
