package ua.company.tzd

import android.app.Application
import android.content.Context
import ua.company.tzd.localization.LocaleManager

/**
 * Клас Application потрібен, щоб якомога раніше підміняти контекст на локалізований варіант.
 * Так усі ресурси одразу відображаються мовою, яку обрав користувач у налаштуваннях.
 */
class TzdApp : Application() {

    override fun attachBaseContext(base: Context?) {
        // Обгортаємо базовий контекст перед ініціалізацією ресурсів, щоб вони одразу знали потрібну локаль.
        val localized = base?.let { LocaleManager.wrapContext(it) }
        super.attachBaseContext(localized)
    }

    override fun onCreate() {
        super.onCreate()
        // Фіксуємо мову в кеші LocaleManager, щоб уникнути повторних звернень до DataStore під час старту.
        LocaleManager.getLanguageSync(this)
    }
}
