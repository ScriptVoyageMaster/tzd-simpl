package ua.company.tzd

import android.app.Application
import ua.company.tzd.settings.SettingsRepository
import ua.company.tzd.settings.UiSettingsManager

/**
 * Клас Application ініціалізує менеджер UI-налаштувань якомога раніше, щоб уся програма стартувала з потрібною темою та мовою.
 */
class TzdApp : Application() {

    /**
     * Публічний доступ до менеджера дозволяє активностям та іншим компонентам гарантувати, що локаль і тема синхронізовані.
     */
    lateinit var uiSettingsManager: UiSettingsManager
        private set

    override fun onCreate() {
        super.onCreate()

        // Створюємо репозиторій один раз на все життя застосунку.
        val repository = SettingsRepository(applicationContext)

        // Менеджер відповідає за застосування теми та мови і слухає зміни в DataStore.
        uiSettingsManager = UiSettingsManager(repository)

        // Синхронна ініціалізація гарантує, що перше значення з DataStore буде застосовано до появи будь-якого UI.
        uiSettingsManager.ensureInitialized()
    }
}
