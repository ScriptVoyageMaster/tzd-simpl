package ua.company.tzd.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Репозиторій зберігає вибір мови та теми в DataStore.
 * Всі методи мають українські коментарі, щоб полегшити підтримку новачкам.
 */
class SettingsRepository(private val context: Context) {

    companion object {
        const val DATASTORE_NAME = "ui_settings"
        private val KEY_LANGUAGE = stringPreferencesKey("ui.language")
        private val KEY_THEME = stringPreferencesKey("ui.theme")
    }

    /**
     * Потік віддає актуальні налаштування користувача і автоматично сповіщає про зміни.
     */
    val settingsFlow: Flow<UiSettings> = context.dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { prefs ->
            UiSettings(
                language = prefs[KEY_LANGUAGE]?.let { AppLanguage.valueOf(it) } ?: AppLanguage.UK,
                theme = prefs[KEY_THEME]?.let { AppTheme.valueOf(it) } ?: AppTheme.LIGHT
            )
        }

    /**
     * Зберігаємо вибір користувача в DataStore. Викликати потрібно з корутини.
     */
    suspend fun save(settings: UiSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = settings.language.name
            prefs[KEY_THEME] = settings.theme.name
        }
    }
}

/**
 * Можливі мови інтерфейсу.
 */
enum class AppLanguage { UK, EN }

/**
 * Можливі теми оформлення.
 */
enum class AppTheme { LIGHT, DARK }

/**
 * Комбінований стан налаштувань, який слухає UI.
 */
data class UiSettings(
    val language: AppLanguage,
    val theme: AppTheme
)

private fun emptyPreferences(): Preferences = androidx.datastore.preferences.core.emptyPreferences()

private val Context.dataStore by preferencesDataStore(name = SettingsRepository.DATASTORE_NAME)
