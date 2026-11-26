plugins {
    alias(libs.plugins.apkanalyzer.core.impl)
    alias(libs.plugins.apkanalyzer.tests.unit)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.appexport.impl"

    androidResources.enable = false

    sourceSets.getByName("test").resources.directories.add("../../../site/inventory-export/v1")
}

dependencies {
    implementation(projects.core.appExport.api)
    implementation(projects.core.apps.api)
    implementation(projects.core.common)

    testImplementation(testFixtures(projects.core.apps.api))
    testImplementation(libs.json.schema.validator)
    testImplementation(libs.kotlinx.serialization.json)
}
