plugins {
    alias(libs.plugins.apkanalyzer.library)
    alias(libs.plugins.apkanalyzer.hilt)
    alias(libs.plugins.apkanalyzer.room)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.aiinsights"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.apps)
    implementation(libs.mlkit.genai.prompt)
}
