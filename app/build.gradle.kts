import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.apkanalyzer.application)
    alias(libs.plugins.apkanalyzer.hilt)
    alias(libs.plugins.apkanalyzer.compose)
    alias(libs.plugins.apkanalyzer.spotless)
    alias(libs.plugins.parcelize)
}

android {
    namespace = "sk.styk.martin.apkanalyzer"

    defaultConfig {
        applicationId = "sk.styk.martin.apkanalyzer"
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

        versionCode = 1
        versionName = "dev"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        generateLocaleConfig = true
    }

    lint {
        disable += "MissingTranslation"
    }
}

dependencies {
    implementation(projects.core.appAnalysisCore)
    implementation(projects.core.appList)
    implementation(projects.core.appPermissions)
    implementation(projects.core.appStatistics)
    implementation(projects.core.common)
    implementation(projects.core.navigation)
    implementation(projects.core.uiLibrary)

    implementation(projects.feature.apps.impl)
    implementation(projects.feature.permissions.impl)
    implementation(projects.feature.statistics.impl)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.collections.immutable)

    debugImplementation(libs.leakcanary)
}
