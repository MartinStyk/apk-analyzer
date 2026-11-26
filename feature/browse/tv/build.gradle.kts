plugins {
    alias(libs.plugins.apkanalyzer.feature.tv)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.browse.tv"
}

dependencies {
    api(projects.feature.browse.api)
    implementation(projects.feature.browse.domain)
    implementation(projects.feature.appDetail.api)
    implementation(projects.core.appIndex)
    implementation(projects.core.appPermissions.api)
    implementation(projects.core.apps.api)
    implementation(projects.core.common)
}
