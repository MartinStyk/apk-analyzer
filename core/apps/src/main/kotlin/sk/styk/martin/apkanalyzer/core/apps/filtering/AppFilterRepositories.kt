package sk.styk.martin.apkanalyzer.core.apps.filtering

interface AppFilterRepositories {
    fun forScope(scope: AppFilterScope): AppFilterRepository
}
