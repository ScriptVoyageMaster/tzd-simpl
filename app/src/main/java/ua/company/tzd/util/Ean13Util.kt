package ua.company.tzd.util

/**
 * Допоміжні методи для перевірки штрих-коду у форматі EAN-13.
 */
object Ean13Util {

    /**
     * Повертає true, якщо код складається з 13 цифр і контрольна сума збігається.
     */
    fun isValid(code: String): Boolean {
        if (code.length != 13 || code.any { !it.isDigit() }) {
            return false
        }
        val digits = code.map { it.digitToInt() }
        val expectedCheckDigit = calculateCheckDigit(digits)
        return expectedCheckDigit == digits[12]
    }

    /**
     * Перевірка та кидання винятку з поясненням, якщо код не валідний.
     */
    fun requireValid(code: String) {
        if (!isValid(code)) {
            throw IllegalArgumentException("EAN-13 check failed")
        }
    }

    private fun calculateCheckDigit(digits: List<Int>): Int {
        val sum = digits.take(12).mapIndexed { index, digit ->
            if ((index + 1) % 2 == 0) digit * 3 else digit
        }.sum()
        return (10 - (sum % 10)) % 10
    }
}
