package sk.styk.martin.apkanalyzer.core.common.applanguage

sealed interface AppLanguageSetting {
    data object Unavailable : AppLanguageSetting

    sealed interface Available : AppLanguageSetting {
        data object SystemDefault : Available
        data class Specific(val displayName: String) : Available
    }
}

interface AppLanguageRepository {
    fun getAppLanguageSetting(): AppLanguageSetting
}
