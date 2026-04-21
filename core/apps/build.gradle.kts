plugins {
    alias(libs.plugins.apkanalyzer.library)
    alias(libs.plugins.apkanalyzer.hilt)
    alias(libs.plugins.parcelize)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.core.applist"
}

dependencies {
    implementation(projects.core.appAnalysisCore)
    api(projects.core.common)
}
