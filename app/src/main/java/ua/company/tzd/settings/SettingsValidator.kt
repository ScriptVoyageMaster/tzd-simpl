package ua.company.tzd.settings

/**
 * Окремий об'єкт із правилами валідації, щоб SettingsActivity могла перевикористовувати логіку.
 */
object SettingsValidator {

    /**
     * Перевіряємо, що всі діапазони у конфігурації коректні та не перетинаються.
     */
    fun isValid(config: ParserConfig): Boolean {
        val parts = listOf(
            config.articleStart to config.articleLength,
            config.kgStart to config.kgLength,
            config.gStart to config.gLength
        )
        if (parts.any { (start, length) -> !isRangeValid(start, length) }) {
            return false
        }

        val ranges = parts.map { (start, length) ->
            val zeroBasedStart = start - 1
            zeroBasedStart until (zeroBasedStart + length)
        }
        return ranges.indices.all { i ->
            ranges.indices.all { j ->
                i == j || !ranges[i].overlaps(ranges[j])
            }
        }
    }

    /**
     * Старт позиції має бути в межах 1..13, а довжина — щонайменше 1.
     */
    fun isRangeValid(start: Int, length: Int): Boolean {
        if (start !in 1..13) return false
        if (length < 1) return false
        val end = start + length - 1
        return end in 1..13
    }

    private fun IntRange.overlaps(other: IntRange): Boolean {
        return first <= other.last && other.first <= last
    }
}
