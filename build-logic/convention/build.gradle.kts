import dev.detekt.gradle.extensions.FailOnSeverity

plugins {
    `kotlin-dsl`
    alias(libs.plugins.detekt)
}

group = "sk.styk.martin.apkanalyzer.buildlogic"

kotlin {
    jvmToolchain(25)
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.spotless.gradle.plugin)
    compileOnly(libs.detekt.gradle.plugin)
}

detekt {
    buildUponDefaultConfig.set(true)
    config.setFrom(layout.projectDirectory.file("../../config/detekt/detekt.yml"))
    basePath.set(layout.projectDirectory.dir("../.."))
    parallel.set(true)
    failOnSeverity.set(FailOnSeverity.Warning)
}

gradlePlugin {
    plugins {
        register("apkanalyzer.agent-context") {
            id = "apkanalyzer.agent-context"
            implementationClass = "sk.styk.martin.apkanalyzer.AgentContextPlugin"
        }
        register("apkanalyzer.application") {
            id = "apkanalyzer.application"
            implementationClass = "sk.styk.martin.apkanalyzer.ApplicationPlugin"
        }
        register("apkanalyzer.library") {
            id = "apkanalyzer.library"
            implementationClass = "sk.styk.martin.apkanalyzer.LibraryPlugin"
        }
        register("apkanalyzer.feature.api") {
            id = "apkanalyzer.feature.api"
            implementationClass = "sk.styk.martin.apkanalyzer.FeatureApiPlugin"
        }
        register("apkanalyzer.feature.impl") {
            id = "apkanalyzer.feature.impl"
            implementationClass = "sk.styk.martin.apkanalyzer.FeatureImplPlugin"
        }
        register("apkanalyzer.hilt") {
            id = "apkanalyzer.hilt"
            implementationClass = "sk.styk.martin.apkanalyzer.HiltPlugin"
        }
        register("apkanalyzer.spotless") {
            id = "apkanalyzer.spotless"
            implementationClass = "sk.styk.martin.apkanalyzer.SpotlessPlugin"
        }
        register("apkanalyzer.detekt") {
            id = "apkanalyzer.detekt"
            implementationClass = "sk.styk.martin.apkanalyzer.DetektPlugin"
        }
        register("apkanalyzer.sarif-merge") {
            id = "apkanalyzer.sarif-merge"
            implementationClass = "sk.styk.martin.apkanalyzer.SarifMergePlugin"
        }
        register("apkanalyzer.compose") {
            id = "apkanalyzer.compose"
            implementationClass = "sk.styk.martin.apkanalyzer.ComposePlugin"
        }
        register("apkanalyzer.room") {
            id = "apkanalyzer.room"
            implementationClass = "sk.styk.martin.apkanalyzer.RoomPlugin"
        }
        register("apkanalyzer.baselineprofile") {
            id = "apkanalyzer.baselineprofile"
            implementationClass = "sk.styk.martin.apkanalyzer.BaselineProfilePlugin"
        }
    }
}
