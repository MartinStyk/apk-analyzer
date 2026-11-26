package sk.styk.martin.apkanalyzer.core.apps.api.filtering

interface AppFilterRepositories {
    fun forScope(scope: AppFilterScope): AppFilterRepository
}
