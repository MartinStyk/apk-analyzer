plugins {
    alias(libs.plugins.apkanalyzer.application)
    alias(libs.plugins.apkanalyzer.hilt)
    alias(libs.plugins.apkanalyzer.compose)
    alias(libs.plugins.apkanalyzer.spotless)
    alias(libs.plugins.apkanalyzer.work)
}

android {
    namespace = "sk.styk.martin.apkanalyzer.tv"

    defaultConfig {
        applicationId = "sk.styk.martin.apkanalyzer"
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        versionCode = (project.findProperty("version.code") as String).toInt()
        versionName = project.findProperty("version.name") as String
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(projects.core.appHistory)
    implementation(projects.core.apps)
    implementation(projects.core.common)
    implementation(projects.core.designSystem.tv)
    implementation(projects.core.navigation)
    implementation(projects.core.userPreferences)

    implementation(projects.feature.apps.tv)
    implementation(projects.feature.appDetail.tv)
    implementation(projects.feature.browse.tv)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.coil.compose)
}
