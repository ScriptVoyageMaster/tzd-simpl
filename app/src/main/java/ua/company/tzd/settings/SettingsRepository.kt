package ua.company.tzd.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.io.IOException

/**
 * Репозиторій зберігає налаштування у DataStore та надає зручні потоки для екрану та логіки.
 * Усі значення проходять через валідацію, щоб не допустити зіпсованих конфігурацій.
 */
class SettingsRepository(private val context: Context) {

    companion object {
        // Назва DataStore та ключі зберігаються тут, щоб не дублювати рядки у коді.
        internal const val DATASTORE_NAME = "settings"

        private val KEY_ARTICLE_START = intPreferencesKey("parser.article.start")
        private val KEY_ARTICLE_LENGTH = intPreferencesKey("parser.article.length")
        private val KEY_KG_START = intPreferencesKey("parser.kg.start")
        private val KEY_KG_LENGTH = intPreferencesKey("parser.kg.length")
        private val KEY_G_START = intPreferencesKey("parser.g.start")
        private val KEY_G_LENGTH = intPreferencesKey("parser.g.length")
        private val KEY_CONFIRM_DELETE = booleanPreferencesKey("ui.confirm_delete")
        private val KEY_ALLOWED_PREFIXES = stringSetPreferencesKey("parser.allowed_prefixes")
        private val PREFIX_REGEX = Regex("\\d{1,13}")

        // Значення за замовчуванням відповідають наданим вимогам.
        val DEFAULT_PARSER = ParserConfig(
            articleStart = 2,
            articleLength = 2,
            kgStart = 8,
            kgLength = 2,
            gStart = 10,
            gLength = 3
        )
        const val DEFAULT_CONFIRM_DELETE = true
        val DEFAULT_ALLOWED_PREFIXES: Set<String> = emptySet()
    }

    /**
     * Потік з повною конфігурацією: розбір коду та прапорець підтвердження видалення.
     */
    val settingsFlow: Flow<SettingsState> = context.settingsDataStore.data
        .catch { error ->
            // Якщо стався збій читання (наприклад, пошкоджений файл), повертаємо дефолти.
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { prefs -> prefs.toSettingsState() }

    /**
     * Зберігаємо у DataStore одразу весь набір параметрів.
     */
    suspend fun saveSettings(config: ParserConfig, confirmDelete: Boolean, allowedPrefixes: Set<String>) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_ARTICLE_START] = config.articleStart
            prefs[KEY_ARTICLE_LENGTH] = config.articleLength
            prefs[KEY_KG_START] = config.kgStart
            prefs[KEY_KG_LENGTH] = config.kgLength
            prefs[KEY_G_START] = config.gStart
            prefs[KEY_G_LENGTH] = config.gLength
            prefs[KEY_CONFIRM_DELETE] = confirmDelete
            prefs[KEY_ALLOWED_PREFIXES] = normalizePrefixes(allowedPrefixes)
        }
    }

    /**
     * Повертаємося до заводських налаштувань, коли користувач натискає кнопку скидання.
     */
    suspend fun resetDefaults() {
        saveSettings(DEFAULT_PARSER, DEFAULT_CONFIRM_DELETE, DEFAULT_ALLOWED_PREFIXES)
    }

    /**
     * Повертаємо актуальний набір дозволених префіксів, очищаючи значення від пробілів і сміття.
     */
    suspend fun getAllowedPrefixes(): Set<String> {
        val raw = context.settingsDataStore.data.first()[KEY_ALLOWED_PREFIXES] ?: DEFAULT_ALLOWED_PREFIXES
        return normalizePrefixes(raw)
    }

    /**
     * Зберігаємо лише чистий набір префіксів, щоб у сховищі не лишалося невалідних записів.
     */
    suspend fun saveAllowedPrefixes(prefixes: Set<String>) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_ALLOWED_PREFIXES] = normalizePrefixes(prefixes)
        }
    }

    /**
     * Конвертуємо сирі дані DataStore у безпечний об'єкт із перевіреними межами.
     */
    private fun Preferences.toSettingsState(): SettingsState {
        val parser = ParserConfig(
            articleStart = get(KEY_ARTICLE_START) ?: DEFAULT_PARSER.articleStart,
            articleLength = get(KEY_ARTICLE_LENGTH) ?: DEFAULT_PARSER.articleLength,
            kgStart = get(KEY_KG_START) ?: DEFAULT_PARSER.kgStart,
            kgLength = get(KEY_KG_LENGTH) ?: DEFAULT_PARSER.kgLength,
            gStart = get(KEY_G_START) ?: DEFAULT_PARSER.gStart,
            gLength = get(KEY_G_LENGTH) ?: DEFAULT_PARSER.gLength
        ).let { config ->
            if (SettingsValidator.isValid(config)) config else DEFAULT_PARSER
        }

        val confirmDelete = get(KEY_CONFIRM_DELETE) ?: DEFAULT_CONFIRM_DELETE
        val prefixes = normalizePrefixes(get(KEY_ALLOWED_PREFIXES) ?: DEFAULT_ALLOWED_PREFIXES)

        return SettingsState(parser, confirmDelete, prefixes)
    }

    /**
     * Дані для споживання у UI: ParserConfig + прапорець підтвердження.
     */
    data class SettingsState(
        val parserConfig: ParserConfig,
        val confirmDelete: Boolean,
        val allowedPrefixes: Set<String>
    )

    /**
     * Приватний хелпер прибирає пробіли, дублікати та пропускає все, що не підпадає під шаблон 1–13 цифр.
     */
    private fun normalizePrefixes(raw: Set<String>): Set<String> {
        val cleaned = raw.mapNotNull { prefix ->
            val compact = prefix.filterNot { it.isWhitespace() }
            if (compact.isEmpty()) return@mapNotNull null
            if (!compact.matches(PREFIX_REGEX)) return@mapNotNull null
            compact
        }.sortedWith(compareBy<String> { it.length }.thenBy { it })
        return LinkedHashSet(cleaned)
    }
}

/**
 * Локальна функція-обгортка над Preferences.empty(), щоб уникнути імпорту в класі.
 */
private fun emptyPreferences(): Preferences = androidx.datastore.preferences.core.emptyPreferences()

/**
 * Створюємо lazy DataStore, що прив'язаний до контексту застосунку.
 */
private val Context.settingsDataStore by preferencesDataStore(name = SettingsRepository.DATASTORE_NAME)
