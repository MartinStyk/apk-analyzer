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

        versionCode = (project.findProperty("version.code") as String).toInt()
        versionName = project.findProperty("version.name") as String
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
    implementation(projects.core.apkFiles)
    implementation(projects.core.apps)
    implementation(projects.core.appPermissions)
    implementation(projects.core.appIndex)
    implementation(projects.core.common)
    implementation(projects.core.userPreferences)
    implementation(projects.core.navigation)
    implementation(projects.core.uiLibrary)

    implementation(projects.feature.apps.impl)
    implementation(projects.feature.browse.impl)
    implementation(projects.feature.settings.impl)
    implementation(projects.feature.appDetail.impl)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.coil.compose)

    debugImplementation(libs.leakcanary)
}
