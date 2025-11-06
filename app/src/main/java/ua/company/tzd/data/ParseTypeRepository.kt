package ua.company.tzd.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Репозиторій відповідає за читання та запис видів парсингу у файл parse_types.json.
 * Методи призначені для корутин, щоб не блокувати основний потік.
 */
class ParseTypeRepository(
    context: Context
) {
    private val storage = FileJsonStorage(context.applicationContext)

    companion object {
        private const val FILE_NAME = "parse_types.json"
    }

    /**
     * Зчитуємо усі схеми парсингу, повертаючи список з дефолтним значенням, якщо файл пустий.
     */
    suspend fun loadAll(): List<ParseType> = withContext(Dispatchers.IO) {
        val json = storage.readOrNull(FILE_NAME) ?: return@withContext emptyList()
        val array = JSONArray(json)
        buildList(array.length()) { index ->
            val obj = array.getJSONObject(index)
            add(obj.toParseType())
        }
    }

    /**
     * Зберігаємо повний список видів, наприклад, після редагування одного елементу.
     */
    suspend fun saveAll(items: List<ParseType>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        items.forEach { type ->
            array.put(type.toJson())
        }
        storage.writeAtomic(FILE_NAME, array.toString())
    }

    /**
     * Створюємо новий об'єкт з автоматичним ідентифікатором.
     */
    fun newParseType(name: String): ParseType {
        val timestamp = System.currentTimeMillis()
        // Коментар для початківця: одразу додаємо мінімальне поле GROUP, щоб нова схема відповідала вимогам конструктора ParseType.
        return ParseType(
            id = UUID.randomUUID().toString(),
            name = name,
            prefixes = emptyList(),
            fields = listOf(createDefaultGroupField()),
            createdAt = timestamp,
            updatedAt = timestamp
        )
    }
}

private fun JSONObject.toParseField(): ParseField {
    return ParseField(
        id = getString("id"),
        title = getString("title"),
        start = getInt("start"),
        length = getInt("length"),
        role = ParseFieldRole.valueOf(getString("role")),
        divisor = getInt("divisor"),
        trimLeadingZeros = optBoolean("trimLeadingZeros", false),
        unit = if (has("unit")) optString("unit") else null
    )
}

private fun ParseField.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("title", title)
        put("start", start)
        put("length", length)
        put("role", role.name)
        put("divisor", divisor)
        put("trimLeadingZeros", trimLeadingZeros)
        unit?.let { put("unit", it) }
    }
}

private fun JSONObject.toParseType(): ParseType {
    val fieldsArray = getJSONArray("fields")
    val fields = buildList(fieldsArray.length()) { index ->
        val fieldObj = fieldsArray.getJSONObject(index)
        add(fieldObj.toParseField())
    }
    return ParseType(
        id = getString("id"),
        name = getString("name"),
        prefixes = optStringList("prefixes"),
        fields = fields,
        createdAt = optLong("createdAt", System.currentTimeMillis()),
        updatedAt = optLong("updatedAt", System.currentTimeMillis())
    )
}

private fun ParseType.toJson(): JSONObject {
    val array = JSONArray()
    fields.forEach { array.put(it.toJson()) }
    return JSONObject().apply {
        put("id", id)
        put("name", name)
        putStringList("prefixes", prefixes)
        put("fields", array)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }
}
