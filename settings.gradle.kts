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
includeBuild("build-logic")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "ApkAnalyzer"
include(
    ":app",
    ":app:baselineprofile",
    ":tv",
    ":core:apk-files:api",
    ":core:apk-files:impl",
    ":core:apps:api",
    ":core:apps:impl",
    ":core:app-permissions:api",
    ":core:app-permissions:impl",
    ":core:app-functions",
    ":core:ai-insights:api",
    ":core:ai-insights:impl",
    ":core:app-index:api",
    ":core:app-index:impl",
    ":core:app-history:api",
    ":core:app-history:impl",
    ":core:app-export:api",
    ":core:app-export:impl",
    ":core:common",
    ":core:entitlements:api",
    ":core:entitlements:impl",
    ":core:entitlements:noop",
    ":core:user-preferences:api",
    ":core:user-preferences:impl",
    ":core:design-system:foundation",
    ":core:design-system:phone",
    ":core:design-system:tv",
    ":core:navigation",
    ":feature:apps:api",
    ":feature:apps:phone",
    ":feature:apps:tv",
    ":feature:app-detail:api",
    ":feature:app-detail:domain",
    ":feature:app-detail:phone",
    ":feature:app-detail:tv",
    ":feature:browse:api",
    ":feature:browse:domain",
    ":feature:browse:phone",
    ":feature:browse:tv",
    ":feature:app-export:api",
    ":feature:app-export:phone",
    ":feature:settings:api",
    ":feature:settings:phone",
    ":feature:what-changed:api",
    ":feature:what-changed:phone",
    ":feature:pro:api",
    ":feature:pro:phone",
)
