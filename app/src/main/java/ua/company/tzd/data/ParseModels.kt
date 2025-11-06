package ua.company.tzd.data

import java.math.BigDecimal

/**
 * Роль поля описує, як трактувати вирізаний із штрих-коду фрагмент.
 */
enum class ParseFieldRole {
    /** Поле, яке визначає групувальний ключ (артикул). */
    GROUP,

    /** Поле, яке потрібно сумувати у підсумках (вага, кількість). */
    SUM,

    /** Додаткова інформація для журналу: партія, серія тощо. */
    INFO
}

/**
 * Опис одного поля в схемі розбору штрих-коду.
 */
data class ParseField(
    val id: String,
    val title: String,
    val start: Int,
    val length: Int,
    val role: ParseFieldRole,
    val divisor: Int,
    val trimLeadingZeros: Boolean,
    val unit: String?
) {
    init {
        require(start >= 1) { "Початкова позиція має бути 1 або більше" }
        require(length >= 1) { "Довжина поля має бути додатною" }
        require(divisor >= 1) { "Дільник має бути не меншим за 1" }
    }
}

/**
 * Повний опис схеми парсингу для певного постачальника/формату.
 */
data class ParseType(
    val id: String,
    val name: String,
    val prefixes: List<String>,
    val fields: List<ParseField>,
    val createdAt: Long,
    val updatedAt: Long
) {
    init {
        require(fields.count { it.role == ParseFieldRole.GROUP } == 1) {
            "У схемі має бути рівно одне групувальне поле"
        }
        require(prefixes.all { it.isNotBlank() }) { "Префікси не повинні бути порожніми" }
    }

    /**
     * Зручний доступ до групувального поля, який часто використовується у парсері.
     */
    val groupField: ParseField = fields.first { it.role == ParseFieldRole.GROUP }
}

/**
 * Опис одного аліаса товару, який прив'язаний до певної схеми парсингу та значення groupKey.
 */
data class ProductAlias(
    val parseTypeId: String,
    val groupKey: String,
    val prefixes: List<String>
)

/**
 * Довідник товару з набором аліасів.
 */
data class Product(
    val id: String,
    val name: String,
    val aliases: List<ProductAlias>,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Рядок підсумку для певного артикула всередині списку.
 */
data class ScanAggregate(
    val groupKey: String,
    val productId: String?,
    val productName: String?,
    val count: Int,
    val sumValues: Map<String, BigDecimal>
)

/**
 * Збережена інформація про список сканування.
 */
data class ScanList(
    val id: String,
    val name: String,
    val parseTypeId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val totalCount: Int,
    val totalSumValues: Map<String, BigDecimal>,
    val aggregates: List<ScanAggregate>
)

/**
 * Статус обробки конкретного скану.
 */
enum class ScanStatus {
    OK,
    ERROR
}

/**
 * Запис журналу сканів для відстеження успішних та помилкових операцій.
 */
data class ScanLogEntry(
    val id: String,
    val timestamp: Long,
    val code: String,
    val status: ScanStatus,
    val statusMessage: String?,
    val parseTypeId: String,
    val groupKey: String?,
    val productName: String?,
    val fieldValues: Map<String, String>
)
