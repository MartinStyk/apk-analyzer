plugins {
    alias(libs.plugins.apkanalyzer.library)
    alias(libs.plugins.apkanalyzer.hilt)
    alias(libs.plugins.apkanalyzer.tests.unit)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.appexport"

    androidResources.enable = false

    sourceSets.getByName("test").resources.directories.add("../../site/inventory-export/v1")
}

dependencies {
    api(projects.core.apps.api)
    implementation(projects.core.common)

    testImplementation(testFixtures(projects.core.apps.api))
    testImplementation(libs.json.schema.validator)
    testImplementation(libs.kotlinx.serialization.json)
}
