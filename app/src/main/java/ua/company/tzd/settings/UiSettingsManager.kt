package ua.company.tzd.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Керівник UI-налаштувань відповідає за централізоване застосування теми та мови по всьому застосунку.
 * Докладні українські коментарі допоможуть навіть новачку розібратися, як відбувається ініціалізація.
 */
class UiSettingsManager(
    private val repository: SettingsRepository,
    private val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {

    /**
     * М'ютекс гарантує, що лише один потік зможе виконати ініціалізацію одночасно.
     */
    private val initMutex = Mutex()

    /**
     * Прапорець показує, що перше налаштування вже застосовано і запущено спостереження змін.
     */
    @Volatile
    private var observing = false

    /**
     * Зберігаємо останні застосовані налаштування, щоб уникати зайвих повторів і не проґавити нові значення.
     */
    @Volatile
    private var lastApplied: UiSettings? = null

    /**
     * Синхронна ініціалізація, яку можна викликати з Application або з будь-якої Activity перед малюванням UI.
     * Виконує блокуючий збір першого елемента, щоб тема й мова збігались із збереженими вподобаннями.
     */
    fun ensureInitialized() {
        if (observing) return
        runBlocking {
            initMutex.withLock {
                if (observing) return@withLock

                // Отримуємо перші налаштування з DataStore та миттєво застосовуємо їх.
                val initialSettings = repository.settingsFlow.first()
                applyTheme(initialSettings.theme)
                applyLanguage(initialSettings.language)
                lastApplied = initialSettings

                observing = true

                // Після першого запуску починаємо слухати всі наступні зміни без повторного блокування потоку.
                startCollectingUpdates()
            }
        }
    }

    /**
     * Викликаємо цей метод, якщо потрібно лише асинхронно запустити спостереження (наприклад, з сервісу).
     */
    suspend fun ensureInitializedAsync() {
        if (observing) return
        initMutex.withLock {
            if (observing) return

            val initialSettings = repository.settingsFlow.first()
            applyTheme(initialSettings.theme)
            applyLanguage(initialSettings.language)
            lastApplied = initialSettings

            observing = true
            startCollectingUpdates()
        }
    }

    /**
     * Запускаємо корутину, яка слухає усі наступні зміни налаштувань і одразу їх застосовує.
     */
    private fun startCollectingUpdates() {
        appScope.launch {
            repository.settingsFlow.collect { settings ->
                if (settings != lastApplied) {
                    applyTheme(settings.theme)
                    applyLanguage(settings.language)
                    lastApplied = settings
                }
            }
        }
    }

    /**
     * Застосовуємо тему. Метод повертає true, якщо режим дійсно змінився.
     */
    fun applyTheme(theme: AppTheme): Boolean {
        val mode = when (theme) {
            AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
        if (AppCompatDelegate.getDefaultNightMode() == mode) {
            return false
        }
        AppCompatDelegate.setDefaultNightMode(mode)
        return true
    }

    /**
     * Застосовуємо мову всього застосунку. Повертає true, якщо локаль змінилася і активності потрібно перезапустити.
     */
    fun applyLanguage(language: AppLanguage): Boolean {
        val locale = when (language) {
            AppLanguage.UK -> Locale("uk")
            AppLanguage.EN -> Locale.ENGLISH
        }
        val localeList = LocaleListCompat.create(locale)
        if (AppCompatDelegate.getApplicationLocales() == localeList) {
            return false
        }
        Locale.setDefault(locale)
        AppCompatDelegate.setApplicationLocales(localeList)
        return true
    }
}
