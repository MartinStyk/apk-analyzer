package sk.styk.martin.apkanalyzer.core.common.applanguage

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class AppLanguageRepositoryImpl @Inject constructor(@ApplicationContext private val context: Context) : AppLanguageRepository {

    override fun getAppLanguageSetting(): AppLanguageSetting {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return AppLanguageSetting.Unavailable

        val locale = context.getSystemService(LocaleManager::class.java)?.applicationLocales?.get(0)
            ?: return AppLanguageSetting.Available.SystemDefault

        return AppLanguageSetting.Available.Specific(locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) })
    }
}
