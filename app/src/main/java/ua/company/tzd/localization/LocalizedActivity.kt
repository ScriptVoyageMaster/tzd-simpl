package ua.company.tzd.localization

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Базова активність автоматично підміняє контекст на локалізований у всіх похідних класах.
 * Щоб не дублювати логіку, всі екрани застосунку мають наслідуватися від цього класу.
 */
open class LocalizedActivity : AppCompatActivity() {

    private var appliedLanguage: String? = null

    override fun attachBaseContext(newBase: Context?) {
        // Передаємо в базовий клас уже локалізований контекст, щоб layout-и одразу отримали правильні рядки.
        val localized = newBase?.let { LocaleManager.wrapContext(it) }
        super.attachBaseContext(localized)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Фіксуємо мову, яка була застосована під час створення, щоб відстежувати подальші зміни.
        appliedLanguage = LocaleManager.getLanguageSync(this)
    }

    override fun onResume() {
        super.onResume()
        // Якщо за час відсутності активності користувач змінив мову, перевідкриваємо екран для оновлення текстів.
        val latestLanguage = LocaleManager.getLanguageSync(this)
        if (appliedLanguage != latestLanguage) {
            appliedLanguage = latestLanguage
            recreate()
        }
    }
}
