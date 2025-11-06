package ua.company.tzd.data

import ua.company.tzd.util.Ean13Util
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.text.toBigDecimalOrNull

/**
 * Клас виконує всю логіку розбору штрих-коду згідно з обраним видом парсингу.
 */
class ScanProcessor(
    private val parseType: ParseType
) {
    private val prefixes: List<String> = parseType.prefixes
    private val sumFields = parseType.fields.filter { it.role == ParseFieldRole.SUM }
    private val infoFields = parseType.fields.filter { it.role == ParseFieldRole.INFO }

    /**
     * Розбираємо код на groupKey і значення полів. Якщо код не валідний — повертаємо помилку.
     */
    fun parse(code: String): ScanResult {
        if (!Ean13Util.isValid(code)) {
            return ScanResult.Error(ScanError.InvalidChecksum)
        }
        if (prefixes.isNotEmpty() && prefixes.none { code.startsWith(it) }) {
            return ScanResult.Error(ScanError.InvalidPrefix)
        }
        return try {
            val groupField = parseType.groupField
            val groupValue = slice(code, groupField)
            if (groupValue.isEmpty()) {
                return ScanResult.Error(ScanError.RequiredFieldEmpty(groupField.title))
            }
            val sums = mutableMapOf<String, BigDecimal>()
            sumFields.forEach { field ->
                val raw = sliceRaw(code, field)
                if (raw.isEmpty()) {
                    return ScanResult.Error(ScanError.RequiredFieldEmpty(field.title))
                }
                val numeric = raw.toBigDecimalOrNull()
                    ?: return ScanResult.Error(ScanError.FieldNotNumeric(field.title))
                val value = numeric.divide(BigDecimal(field.divisor), 6, RoundingMode.HALF_UP)
                sums[field.id] = value.stripTrailingZeros()
            }
            val infos = mutableMapOf<String, String>()
            infoFields.forEach { field ->
                val raw = slice(code, field)
                infos[field.id] = raw.ifEmpty { "0" }
            }
            ScanResult.Success(
                code = code,
                groupKey = groupValue,
                sumValues = sums,
                infoValues = infos
            )
        } catch (ex: IndexOutOfBoundsException) {
            ScanResult.Error(ScanError.FieldOutOfRange)
        }
    }

    private fun slice(code: String, field: ParseField): String {
        val startIndex = field.start - 1
        val endIndex = startIndex + field.length
        val raw = code.substring(startIndex, endIndex)
        return if (field.trimLeadingZeros) raw.trimStart('0') else raw
    }

    private fun sliceRaw(code: String, field: ParseField): String {
        val startIndex = field.start - 1
        val endIndex = startIndex + field.length
        return code.substring(startIndex, endIndex)
    }
}

/**
 * Типи помилок, що можуть виникнути під час розбору.
 */
sealed interface ScanError {
    object InvalidChecksum : ScanError
    object InvalidPrefix : ScanError
    object FieldOutOfRange : ScanError
    data class RequiredFieldEmpty(val fieldTitle: String) : ScanError
    data class FieldNotNumeric(val fieldTitle: String) : ScanError
}

/**
 * Результат розбору: успіх із даними або помилка.
 */
sealed interface ScanResult {
    data class Success(
        val code: String,
        val groupKey: String,
        val sumValues: Map<String, BigDecimal>,
        val infoValues: Map<String, String>
    ) : ScanResult

    data class Error(val error: ScanError) : ScanResult
}
