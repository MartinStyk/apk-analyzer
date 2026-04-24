plugins {
    alias(libs.plugins.apkanalyzer.library)
    alias(libs.plugins.apkanalyzer.hilt)
    alias(libs.plugins.apkanalyzer.compose)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.uilibrary"
}

dependencies {
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.coil.core)
    implementation(libs.coil.compose)
    implementation(libs.androidx.navigation3.ui)
    implementation(projects.core.common)
}
