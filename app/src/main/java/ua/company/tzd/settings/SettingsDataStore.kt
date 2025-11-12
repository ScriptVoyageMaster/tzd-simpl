package ua.company.tzd.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * Спільний провайдер DataStore, щоб одна й та сама база налаштувань використовувалася в різних компонентах.
 */
internal val Context.settingsDataStore by preferencesDataStore(name = SettingsRepository.DATASTORE_NAME)
