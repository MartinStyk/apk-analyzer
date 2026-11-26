plugins {
    alias(libs.plugins.apkanalyzer.core.impl)
    alias(libs.plugins.apkanalyzer.room)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.aiinsights.impl"

    androidResources.enable = false
}

dependencies {
    implementation(projects.core.aiInsights.api)
    implementation(projects.core.apps.api)
    implementation(projects.core.common)
    implementation(libs.mlkit.genai.prompt)
}
