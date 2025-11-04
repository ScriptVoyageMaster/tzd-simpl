package ua.company.tzd.util

import ua.company.tzd.settings.ParserConfig

/**
 * Утилітарний об'єкт відповідає за коректний розбір EAN-13 на артикул та вагу.
 */
object ParserUtil {

    /**
     * Повертає артикул, кілограми та грами, якщо код відповідає EAN-13 і налаштування валідні.
     * У разі проблем кидаємо IllegalArgumentException, щоб виклик може вирішити, що робити.
     */
    fun extractArticleKgG(code: String, cfg: ParserConfig): Triple<String, Int, Int> {
        // Перевірка довжини й наявності лише цифр.
        if (code.length != 13 || code.any { !it.isDigit() }) {
            throw IllegalArgumentException("Code is not EAN-13")
        }

        // Обчислюємо контрольну цифру та переконуємося, що код справді валідний.
        val digits = code.map { it.digitToInt() }
        val sum = digits.take(12).mapIndexed { index, digit ->
            if ((index + 1) % 2 == 0) digit * 3 else digit
        }.sum()
        val expectedCheckDigit = (10 - (sum % 10)) % 10
        if (expectedCheckDigit != digits[12]) {
            throw IllegalArgumentException("EAN-13 check digit mismatch")
        }

        // Розрізаємо рядок згідно 1-індексованих позицій з конфігурації.
        val article = try {
            code.sliceSubstring(cfg.articleStart, cfg.articleLength)
        } catch (ex: IndexOutOfBoundsException) {
            throw IllegalArgumentException("Article range out of bounds", ex)
        }
        val kgStr = try {
            code.sliceSubstring(cfg.kgStart, cfg.kgLength)
        } catch (ex: IndexOutOfBoundsException) {
            throw IllegalArgumentException("Kilogram range out of bounds", ex)
        }
        val gStr = try {
            code.sliceSubstring(cfg.gStart, cfg.gLength)
        } catch (ex: IndexOutOfBoundsException) {
            throw IllegalArgumentException("Gram range out of bounds", ex)
        }

        val kg = kgStr.toIntOrNull() ?: throw IllegalArgumentException("Kilograms not numeric")
        val g = gStr.toIntOrNull() ?: throw IllegalArgumentException("Grams not numeric")

        return Triple(article, kg, g)
    }

    /**
     * Допоміжна функція для роботи з 1-індексацією (start = 1 означає перший символ).
     */
    private fun String.sliceSubstring(startOneBased: Int, length: Int): String {
        val startIndex = startOneBased - 1
        val endIndexExclusive = startIndex + length
        return substring(startIndex, endIndexExclusive)
    }
}
