package ua.company.tzd.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.util.UUID

/**
 * Репозиторій для списків сканування та агрегованих даних.
 */
class ScanListRepository(
    context: Context
) {
    private val storage = FileJsonStorage(context.applicationContext)

    companion object {
        private const val FILE_NAME = "scan_lists.json"
    }

    suspend fun loadAll(): List<ScanList> = withContext(Dispatchers.IO) {
        val json = storage.readOrNull(FILE_NAME) ?: return@withContext emptyList()
        val array = JSONArray(json)
        buildList(array.length()) { index ->
            add(array.getJSONObject(index).toScanList())
        }
    }

    suspend fun saveAll(items: List<ScanList>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        items.forEach { array.put(it.toJson()) }
        storage.writeAtomic(FILE_NAME, array.toString())
    }

    fun newList(name: String, parseTypeId: String): ScanList {
        val timestamp = System.currentTimeMillis()
        return ScanList(
            id = UUID.randomUUID().toString(),
            name = name,
            parseTypeId = parseTypeId,
            createdAt = timestamp,
            updatedAt = timestamp,
            totalCount = 0,
            totalSumValues = emptyMap(),
            aggregates = emptyList()
        )
    }
}

private fun JSONObject.toScanAggregate(): ScanAggregate {
    val sums = optBigDecimalMap("sumValues")
    return ScanAggregate(
        groupKey = getString("groupKey"),
        productId = optString("productId").takeIf { has("productId") },
        productName = optString("productName").takeIf { has("productName") },
        count = getInt("count"),
        sumValues = sums
    )
}

private fun ScanAggregate.toJson(): JSONObject {
    return JSONObject().apply {
        put("groupKey", groupKey)
        productId?.let { put("productId", it) }
        productName?.let { put("productName", it) }
        put("count", count)
        putBigDecimalMap("sumValues", sumValues)
    }
}

private fun JSONObject.toScanList(): ScanList {
    val aggregatesArray = optJSONArray("aggregates") ?: JSONArray()
    val aggregates = buildList(aggregatesArray.length()) { index ->
        add(aggregatesArray.getJSONObject(index).toScanAggregate())
    }
    return ScanList(
        id = getString("id"),
        name = getString("name"),
        parseTypeId = getString("parseTypeId"),
        createdAt = optLong("createdAt", System.currentTimeMillis()),
        updatedAt = optLong("updatedAt", System.currentTimeMillis()),
        totalCount = optInt("totalCount", 0),
        totalSumValues = optBigDecimalMap("totalSumValues"),
        aggregates = aggregates
    )
}

private fun ScanList.toJson(): JSONObject {
    val aggregatesArray = JSONArray()
    aggregates.forEach { aggregatesArray.put(it.toJson()) }
    return JSONObject().apply {
        put("id", id)
        put("name", name)
        put("parseTypeId", parseTypeId)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("totalCount", totalCount)
        putBigDecimalMap("totalSumValues", totalSumValues)
        put("aggregates", aggregatesArray)
    }
}
