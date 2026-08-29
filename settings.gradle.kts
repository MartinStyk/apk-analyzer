pluginManagement {
    includeBuild("build-logic")

    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
    }
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "ApkAnalyzer"
include(
    ":app",
    ":app:baselineprofile",
    ":core:apk-files",
    ":core:apps",
    ":core:app-permissions",
    ":core:app-functions",
    ":core:ai-insights",
    ":core:app-index",
    ":core:common",
    ":core:user-preferences",
    ":core:ui-library",
    ":core:navigation",
    ":feature:apps:api",
    ":feature:apps:impl",
    ":feature:app-detail:api",
    ":feature:app-detail:impl",
    ":feature:browse:api",
    ":feature:browse:impl",
    ":feature:settings:api",
    ":feature:settings:impl",
)
