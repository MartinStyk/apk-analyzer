pluginManagement {
    includeBuild("build-logic")

    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
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
    ":core:app-analysis-core",
    ":core:apps",
    ":core:app-permissions",
    ":core:app-statistics",
    ":core:common",
    ":core:ui-library",
    ":core:navigation",
    ":feature:apps:api",
    ":feature:apps:impl",
    ":feature:statistics:api",
    ":feature:statistics:impl",
    ":feature:permissions:api",
    ":feature:permissions:impl",
    ":feature:settings:api",
    ":feature:settings:impl",
)
