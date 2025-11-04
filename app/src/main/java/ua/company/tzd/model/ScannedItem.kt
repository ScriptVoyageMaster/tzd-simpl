package ua.company.tzd.model

/**
 * Збережена інформація про один відсканований штрих-код.
 * Зверніть увагу: артикул та код зберігаємо як рядки, щоб не втратити провідні нулі.
 */
data class ScannedItem(
    val code: String,
    val article: String,
    val kg: Int,
    val g: Int,
    val time: Long
)
