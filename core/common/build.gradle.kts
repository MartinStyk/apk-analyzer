plugins {
    alias(libs.plugins.apkanalyzer.library)
    alias(libs.plugins.apkanalyzer.hilt)
    alias(libs.plugins.apkanalyzer.tests.unit)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.common"

    buildFeatures {
        buildConfig = true
    }

    androidResources.enable = false
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.performance)
    implementation(libs.timber)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.play.review.ktx)
    implementation(libs.androidx.core.ktx)
}
