package ua.company.tzd.model

/**
 * Рядок підсумкової статистики по окремому артикулу.
 * Грами завжди нормалізовані до діапазону 0..999.
 */
data class SummaryItem(
    val article: String,
    val count: Int,
    val kg: Int,
    val g: Int,
    val latestTime: Long
)
