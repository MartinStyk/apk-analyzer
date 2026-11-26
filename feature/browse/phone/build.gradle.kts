plugins {
    alias(libs.plugins.apkanalyzer.feature.phone)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.browse.phone"
}

dependencies {
    api(projects.feature.browse.api)
    implementation(projects.feature.browse.domain)
    implementation(projects.feature.appDetail.api)
    implementation(projects.core.appIndex.api)
    implementation(projects.core.apps.api)
    implementation(projects.core.appPermissions.api)
    implementation(projects.core.common)
}
