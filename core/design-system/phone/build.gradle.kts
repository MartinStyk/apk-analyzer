plugins {
    alias(libs.plugins.apkanalyzer.library)
    alias(libs.plugins.apkanalyzer.hilt)
    alias(libs.plugins.apkanalyzer.compose)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.designsystem.phone"
}

dependencies {
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation3.ui)
    api(projects.core.designSystem.foundation)
    implementation(projects.core.common)
}
