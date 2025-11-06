package ua.company.tzd.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Репозиторій для журналу конкретного списку. Кожен список має власний файл.
 */
class ScanLogRepository(
    context: Context
) {
    private val storage = FileJsonStorage(context.applicationContext)

    /**
     * Читаємо журнал для вказаного списку. Якщо журналу немає — повертаємо порожній список.
     */
    suspend fun load(listId: String): List<ScanLogEntry> = withContext(Dispatchers.IO) {
        val fileName = fileName(listId)
        val json = storage.readOrNull(fileName) ?: return@withContext emptyList()
        val array = JSONArray(json)
        buildList(array.length()) { index ->
            add(array.getJSONObject(index).toLogEntry())
        }
    }

    /**
     * Зберігаємо повний журнал (метод використовується після очищення або масового оновлення).
     */
    suspend fun save(listId: String, entries: List<ScanLogEntry>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        entries.forEach { array.put(it.toJson()) }
        storage.writeAtomic(fileName(listId), array.toString())
    }

    /**
     * Додаємо один запис у кінець журналу. Читаємо попередній вміст і додаємо новий елемент.
     */
    suspend fun append(listId: String, entry: ScanLogEntry) = withContext(Dispatchers.IO) {
        val entries = load(listId).toMutableList()
        entries.add(entry)
        save(listId, entries)
    }

    /**
     * Видаляємо файл журналу, коли список повністю стерто.
     */
    suspend fun delete(listId: String) = withContext(Dispatchers.IO) {
        storage.delete(fileName(listId))
    }

    fun newEntry(
        status: ScanStatus,
        code: String,
        parseTypeId: String,
        groupKey: String?,
        productName: String?,
        fieldValues: Map<String, String>,
        statusMessage: String?
    ): ScanLogEntry {
        val timestamp = System.currentTimeMillis()
        return ScanLogEntry(
            id = UUID.randomUUID().toString(),
            timestamp = timestamp,
            code = code,
            status = status,
            statusMessage = statusMessage,
            parseTypeId = parseTypeId,
            groupKey = groupKey,
            productName = productName,
            fieldValues = fieldValues
        )
    }

    private fun fileName(listId: String): String = "scan_logs_${'$'}listId.json"
}

private fun JSONObject.toLogEntry(): ScanLogEntry {
    val fields = mutableMapOf<String, String>()
    val fieldsObj = optJSONObject("fieldValues")
    fieldsObj?.keys()?.forEachRemaining { key ->
        fields[key] = fieldsObj.optString(key)
    }
    return ScanLogEntry(
        id = getString("id"),
        timestamp = optLong("timestamp", System.currentTimeMillis()),
        code = getString("code"),
        status = ScanStatus.valueOf(getString("status")),
        statusMessage = optString("statusMessage").takeIf { has("statusMessage") },
        parseTypeId = getString("parseTypeId"),
        groupKey = optString("groupKey").takeIf { has("groupKey") },
        productName = optString("productName").takeIf { has("productName") },
        fieldValues = fields
    )
}

private fun ScanLogEntry.toJson(): JSONObject {
    val fieldsObj = JSONObject()
    fieldValues.forEach { (key, value) -> fieldsObj.put(key, value) }
    return JSONObject().apply {
        put("id", id)
        put("timestamp", timestamp)
        put("code", code)
        put("status", status.name)
        statusMessage?.let { put("statusMessage", it) }
        put("parseTypeId", parseTypeId)
        groupKey?.let { put("groupKey", it) }
        productName?.let { put("productName", it) }
        put("fieldValues", fieldsObj)
    }
}
