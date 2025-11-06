package ua.company.tzd.data

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

/**
 * Допоміжні розширення для спрощення роботи з JSONObject/JSONArray.
 */
internal fun JSONObject.optStringList(key: String): List<String> {
    if (!has(key)) return emptyList()
    val array = optJSONArray(key) ?: return emptyList()
    return buildList(array.length()) { index ->
        add(array.optString(index))
    }
}

internal fun JSONArray.toStringList(): List<String> {
    return buildList(length()) { index ->
        add(optString(index))
    }
}

internal fun JSONObject.putStringList(key: String, values: Collection<String>) {
    put(key, JSONArray().apply { values.forEach { put(it) } })
}

internal fun JSONObject.optBigDecimalMap(key: String): Map<String, BigDecimal> {
    if (!has(key)) return emptyMap()
    val obj = optJSONObject(key) ?: return emptyMap()
    val result = mutableMapOf<String, BigDecimal>()
    obj.keys().forEachRemaining { fieldId ->
        val value = obj.optString(fieldId)
        if (value.isNotBlank()) {
            result[fieldId] = value.toBigDecimal()
        }
    }
    return result
}

internal fun JSONObject.putBigDecimalMap(key: String, map: Map<String, BigDecimal>) {
    val obj = JSONObject()
    map.forEach { (fieldId, value) ->
        obj.put(fieldId, value.stripTrailingZeros().toPlainString())
    }
    put(key, obj)
}
