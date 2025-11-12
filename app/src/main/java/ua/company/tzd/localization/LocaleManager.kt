package ua.company.tzd.localization

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import ua.company.tzd.settings.settingsDataStore
import java.util.Locale

/**
 * Менеджер локалей відповідає за збереження вибраної мови та підстановку локалізованого контексту.
 * Використовуємо DataStore, щоб налаштування переживало перезапуск застосунку.
 */
object LocaleManager {

    const val LANGUAGE_UK = "uk"
    const val LANGUAGE_EN = "en"
    private const val DEFAULT_LANGUAGE = LANGUAGE_UK

    private val SUPPORTED_LANGUAGES = setOf(LANGUAGE_UK, LANGUAGE_EN)
    private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")

    @Volatile
    private var cachedLanguage: String? = null

    /**
     * Повертаємо поточну мову як потік, щоб екрани могли реагувати на зміни у налаштуваннях.
     */
    fun languageFlow(context: Context): Flow<String> {
        return context.settingsDataStore.data
            .map { prefs -> normalizeLanguage(prefs[KEY_APP_LANGUAGE]) }
            .distinctUntilChanged()
    }

    /**
     * Зберігаємо нову мову у DataStore та одразу оновлюємо кеш, щоб повторні звернення були миттєвими.
     */
    suspend fun setLanguage(context: Context, language: String) {
        val normalized = normalizeLanguage(language)
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_APP_LANGUAGE] = normalized
        }
        cachedLanguage = normalized
    }

    /**
     * Асинхронно зчитуємо поточну мову. Якщо кеш уже заповнений, повертаємо його без доступу до диска.
     */
    suspend fun getLanguage(context: Context): String {
        cachedLanguage?.let { return it }
        val stored = context.settingsDataStore.data.first()[KEY_APP_LANGUAGE]
        val normalized = normalizeLanguage(stored)
        cachedLanguage = normalized
        return normalized
    }

    /**
     * Синхронне читання мови потрібне під час attachBaseContext, де немає змоги викликати suspend-функцію.
     */
    fun getLanguageSync(context: Context): String {
        cachedLanguage?.let { return it }
        val normalized = runBlocking { getLanguage(context) }
        cachedLanguage = normalized
        return normalized
    }

    /**
     * Обгортаємо будь-який контекст, застосовуючи локалізовану конфігурацію ресурсів.
     */
    fun wrapContext(context: Context): Context {
        val language = getLanguageSync(context)
        return applyLocale(context, language)
    }

    /**
     * Використовується після зміни мови, щоб одразу отримати об'єкт Locale для форматування дат чи чисел.
     */
    fun currentLocale(context: Context): Locale {
        return Locale(getLanguageSync(context))
    }

    /**
     * Надаємо базову мову за замовчуванням для ініціалізації перемикачів.
     */
    fun defaultLanguage(): String = DEFAULT_LANGUAGE

    private fun applyLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            configuration.setLayoutDirection(locale)
            context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            run {
                configuration.locale = locale
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    configuration.setLayoutDirection(locale)
                }
                @Suppress("DEPRECATION")
                resources.updateConfiguration(configuration, resources.displayMetrics)
                context
            }
        }
    }

    private fun normalizeLanguage(language: String?): String {
        val candidate = language?.lowercase(Locale.ROOT)
        return if (candidate != null && SUPPORTED_LANGUAGES.contains(candidate)) {
            candidate
        } else {
            DEFAULT_LANGUAGE
        }
    }
}
