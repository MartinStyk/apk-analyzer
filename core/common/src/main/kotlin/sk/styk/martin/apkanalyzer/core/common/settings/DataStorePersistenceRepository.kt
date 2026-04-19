package sk.styk.martin.apkanalyzer.core.common.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val KEY_COLOR_SCHEME = stringPreferencesKey("dayNightPref")
private val KEY_ONBOARDING = booleanPreferencesKey("first_app_start")
private val KEY_APP_START_NUMBER = intPreferencesKey("app_start_number")

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = "sk.styk.martin.apkanalyzer_preferences",
                keysToMigrate =
                setOf(
                    KEY_COLOR_SCHEME.name,
                    KEY_ONBOARDING.name,
                    KEY_APP_START_NUMBER.name,
                ),
            ),
        )
    },
)

@Suppress("UNCHECKED_CAST")
@Singleton
class DataStorePersistenceRepository
@Inject
constructor(@param:ApplicationContext private val context: Context) : PersistenceRepository {
    override fun <T> observe(key: Key<T>): Flow<T> = context.dataStore.data.map { prefs -> key.read(prefs) }

    override suspend fun <T> save(key: Key<T>, value: T) {
        context.dataStore.edit { prefs -> key.write(prefs, value) }
    }

    private fun <T> Key<T>.read(prefs: Preferences): T = when (this) {
        Key.ColorScheme -> {
            when (prefs[KEY_COLOR_SCHEME]) {
                "light" -> ColorAppScheme.Day
                "dark" -> ColorAppScheme.Night
                else -> ColorAppScheme.FollowSystem
            }
        }

        Key.OnboardingRequired -> {
            prefs[KEY_ONBOARDING] ?: true
        }

        Key.AppStartNumber -> {
            prefs[KEY_APP_START_NUMBER] ?: 0
        }
    } as T

    private fun <T> Key<T>.write(prefs: MutablePreferences, value: T) = when (this) {
        Key.ColorScheme -> {
            prefs[KEY_COLOR_SCHEME] =
                when (value as ColorAppScheme) {
                    ColorAppScheme.Day -> "light"
                    ColorAppScheme.Night -> "dark"
                    ColorAppScheme.FollowSystem -> "system"
                }
        }

        Key.OnboardingRequired -> {
            prefs[KEY_ONBOARDING] = value as Boolean
        }

        Key.AppStartNumber -> {
            prefs[KEY_APP_START_NUMBER] = value as Int
        }
    }
}
