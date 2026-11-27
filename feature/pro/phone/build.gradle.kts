plugins {
    alias(libs.plugins.apkanalyzer.feature.phone)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.feature.pro.phone"
}

dependencies {
    api(projects.feature.pro.api)
    implementation(projects.core.entitlements.api)
    implementation(projects.core.common)
    implementation(libs.androidx.activity.compose)
}
