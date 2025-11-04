package ua.company.tzd.util

import ua.company.tzd.model.ScannedItem
import ua.company.tzd.model.SummaryItem

/**
 * Допоміжні функції для підрахунку статистики без використання плаваючої арифметики.
 */
object TotalsUtil {

    /**
     * Групуємо елементи за артикулом та рахуємо сумарну вагу й кількість у кожній групі.
     */
    fun groupByArticle(items: List<ScannedItem>): List<SummaryItem> {
        val grouped = items.groupBy { it.article }
        return grouped.map { (article, articleItems) ->
            var kgSum = 0
            var gSum = 0
            var latestTime = 0L
            articleItems.forEach { item ->
                kgSum += item.kg
                gSum += item.g
                if (item.time > latestTime) {
                    latestTime = item.time
                }
            }
            kgSum += gSum / 1000
            gSum %= 1000
            SummaryItem(
                article = article,
                count = articleItems.size,
                kg = kgSum,
                g = gSum,
                latestTime = latestTime
            )
        }.sortedByDescending { it.latestTime }
    }

    /**
     * Обчислюємо загальний підсумок ваги за всіма артикулами.
     */
    fun calcGrandTotal(summary: List<SummaryItem>): Pair<Int, Int> {
        var kgSum = 0
        var gSum = 0
        summary.forEach { item ->
            kgSum += item.kg
            gSum += item.g
        }
        kgSum += gSum / 1000
        gSum %= 1000
        return kgSum to gSum
    }
}
