package sk.styk.martin.apkanalyzer.utils

import com.android.build.api.dsl.Lint

internal fun Lint.configureLint() {
    warningsAsErrors = true
    ignoreTestFixturesSources = true
    disable += setOf(
        "AndroidGradlePluginVersion",
        "GradleDependency",
        "NewerVersionAvailable",
        "IconMissingDensityFolder",
    )
}
