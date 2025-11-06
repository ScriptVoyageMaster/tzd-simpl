package ua.company.tzd

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import ua.company.tzd.databinding.ActivitySettingsBinding
import ua.company.tzd.settings.AppLanguage
import ua.company.tzd.settings.AppTheme
import ua.company.tzd.settings.SettingsRepository
import ua.company.tzd.settings.UiSettings
import ua.company.tzd.settings.UiSettingsManager

/**
 * Новий екран "Налаштування" дозволяє змінити мову інтерфейсу та тему застосунку.
 * Усі коментарі українською, щоб навіть новачок зрозумів логіку крок за кроком.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repository: SettingsRepository
    private val uiSettingsManager: UiSettingsManager by lazy { (application as TzdApp).uiSettingsManager }
    private var currentSettings: UiSettings = UiSettings(AppLanguage.UK, AppTheme.LIGHT)
    private var applyingUiState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Перед створенням інтерфейсу переконуємося, що глобальні налаштування вже застосовані.
        uiSettingsManager.ensureInitialized()

        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = SettingsRepository(applicationContext)

        setupToolbar()
        setupListeners()
        observeSettings()
    }

    /**
     * Верхній заголовок допомагає користувачу швидко зорієнтуватися, де він знаходиться.
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.settings_title)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    /**
     * Підписуємося на DataStore, щоб миттєво реагувати на зміни налаштувань.
     */
    private fun observeSettings() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                repository.settingsFlow.collect { settings ->
                    currentSettings = settings
                    applyUiState(settings)
                    applyTheme(settings.theme)
                    applyLanguage(settings.language)
                }
            }
        }
    }

    /**
     * Задаємо слухачі для перемикачів. Коли користувач змінює значення — зберігаємо у DataStore.
     */
    private fun setupListeners() {
        binding.radioLanguage.setOnCheckedChangeListener { _, checkedId ->
            if (applyingUiState) return@setOnCheckedChangeListener
            val language = when (checkedId) {
                R.id.radio_language_uk -> AppLanguage.UK
                R.id.radio_language_en -> AppLanguage.EN
                else -> currentSettings.language
            }
            saveSettings(currentSettings.copy(language = language))
        }
        binding.radioTheme.setOnCheckedChangeListener { _, checkedId ->
            if (applyingUiState) return@setOnCheckedChangeListener
            val theme = when (checkedId) {
                R.id.radio_theme_light -> AppTheme.LIGHT
                R.id.radio_theme_dark -> AppTheme.DARK
                else -> currentSettings.theme
            }
            saveSettings(currentSettings.copy(theme = theme))
        }
    }

    /**
     * Застосовуємо стан у UI без виклику зайвих колбеків (applyingUiState = true).
     */
    private fun applyUiState(settings: UiSettings) {
        applyingUiState = true
        binding.radioLanguage.check(
            when (settings.language) {
                AppLanguage.UK -> R.id.radio_language_uk
                AppLanguage.EN -> R.id.radio_language_en
            }
        )
        binding.radioTheme.check(
            when (settings.theme) {
                AppTheme.LIGHT -> R.id.radio_theme_light
                AppTheme.DARK -> R.id.radio_theme_dark
            }
        )
        applyingUiState = false
    }

    /**
     * Окремий метод для збереження, щоб не дублювати запуск корутини.
     */
    private fun saveSettings(settings: UiSettings) {
        lifecycleScope.launch {
            repository.save(settings)
        }
    }

    /**
     * Застосовуємо тему через спільний менеджер, який всередині користується AppCompatDelegate.
     */
    private fun applyTheme(theme: AppTheme) {
        // Делегуємо застосування теми спільному менеджеру, щоб уникнути дублювання логіки.
        uiSettingsManager.applyTheme(theme)
    }

    /**
     * Зміна мови через менеджер; якщо локаль змінилася — перезапускаємо активність, щоб підтягнути строки.
     */
    private fun applyLanguage(language: AppLanguage) {
        // Менеджер повертає true, якщо локаль дійсно змінилася, і тоді Activity перезапускається.
        val localeChanged = uiSettingsManager.applyLanguage(language)
        if (localeChanged) {
            recreate()
        }
    }
}
