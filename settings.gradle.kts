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
    ":tv",
    ":core:apk-files",
    ":core:apps",
    ":core:app-permissions",
    ":core:app-functions",
    ":core:ai-insights",
    ":core:app-index",
    ":core:app-history",
    ":core:app-export",
    ":core:common",
    ":core:user-preferences",
    ":core:design-system:foundation",
    ":core:design-system:phone",
    ":core:design-system:tv",
    ":core:navigation",
    ":feature:apps:api",
    ":feature:apps:phone",
    ":feature:apps:tv",
    ":feature:app-detail:api",
    ":feature:app-detail:phone",
    ":feature:app-detail:tv",
    ":feature:browse:api",
    ":feature:browse:phone",
    ":feature:browse:tv",
    ":feature:settings:api",
    ":feature:settings:phone",
)
