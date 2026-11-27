import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.apkanalyzer.application)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.apkanalyzer.hilt)
    alias(libs.plugins.apkanalyzer.compose)
    alias(libs.plugins.apkanalyzer.spotless)
    alias(libs.plugins.apkanalyzer.appfunctions)
    alias(libs.plugins.apkanalyzer.work)
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
}

baselineProfile {
    automaticGenerationDuringBuild = false
}

dependencies {
    baselineProfile(projects.app.baselineprofile)

    implementation(projects.core.apkFiles.api)
    implementation(projects.core.apkFiles.impl)
    implementation(projects.core.apps.api)
    implementation(projects.core.apps.impl)
    implementation(projects.core.appHistory.api)
    implementation(projects.core.appHistory.impl)
    implementation(projects.core.appExport.api)
    implementation(projects.core.appExport.impl)
    implementation(projects.core.aiInsights.api)
    implementation(projects.core.aiInsights.impl)
    implementation(projects.core.userPreferences.api)
    implementation(projects.core.userPreferences.impl)
    implementation(projects.core.appIndex.api)
    implementation(projects.core.appIndex.impl)
    implementation(projects.core.appPermissions.api)
    implementation(projects.core.appPermissions.impl)
    implementation(projects.core.entitlements.api)
    debugImplementation(projects.core.entitlements.impl)
    releaseImplementation(projects.core.entitlements.noop)
    implementation(projects.core.appFunctions)
    implementation(projects.core.common)
    implementation(projects.core.navigation)
    implementation(projects.core.designSystem.phone)

    implementation(projects.feature.apps.phone)
    implementation(projects.feature.browse.phone)
    implementation(projects.feature.appExport.phone)
    implementation(projects.feature.settings.phone)
    implementation(projects.feature.appDetail.phone)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.compose.material3)
    implementation(libs.coil.compose)

    debugImplementation(libs.leakcanary)
}

ksp { arg("appfunctions:aggregateAppFunctions", "true") }
