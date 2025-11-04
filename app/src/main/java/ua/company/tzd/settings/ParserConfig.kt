package ua.company.tzd.settings

/**
 * Конфігурація для розбору штрих-коду EAN-13 згідно позицій користувача.
 * Старт та довжина зберігаються у 1-індексованому форматі, щоб збігатися з описом вимог.
 */
data class ParserConfig(
    val articleStart: Int,
    val articleLength: Int,
    val kgStart: Int,
    val kgLength: Int,
    val gStart: Int,
    val gLength: Int
)
