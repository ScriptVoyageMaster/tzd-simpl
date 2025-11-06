package ua.company.tzd.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Репозиторій для роботи з довідником товарів, що зберігається у файлі products.json.
 */
class ProductRepository(
    context: Context
) {
    private val storage = FileJsonStorage(context.applicationContext)

    companion object {
        private const val FILE_NAME = "products.json"
    }

    suspend fun loadAll(): List<Product> = withContext(Dispatchers.IO) {
        val json = storage.readOrNull(FILE_NAME) ?: return@withContext emptyList()
        val array = JSONArray(json)
        buildList(array.length()) { index ->
            add(array.getJSONObject(index).toProduct())
        }
    }

    suspend fun saveAll(items: List<Product>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        items.forEach { array.put(it.toJson()) }
        storage.writeAtomic(FILE_NAME, array.toString())
    }

    fun newProduct(name: String): Product {
        val timestamp = System.currentTimeMillis()
        return Product(
            id = UUID.randomUUID().toString(),
            name = name,
            aliases = emptyList(),
            createdAt = timestamp,
            updatedAt = timestamp
        )
    }
}

private fun JSONObject.toAlias(): ProductAlias {
    return ProductAlias(
        parseTypeId = getString("parseTypeId"),
        groupKey = getString("groupKey"),
        prefixes = optStringList("prefixes")
    )
}

private fun ProductAlias.toJson(): JSONObject {
    return JSONObject().apply {
        put("parseTypeId", parseTypeId)
        put("groupKey", groupKey)
        putStringList("prefixes", prefixes)
    }
}

private fun JSONObject.toProduct(): Product {
    val aliasesArray = optJSONArray("aliases") ?: JSONArray()
    val aliases = buildList(aliasesArray.length()) { index ->
        add(aliasesArray.getJSONObject(index).toAlias())
    }
    return Product(
        id = getString("id"),
        name = getString("name"),
        aliases = aliases,
        createdAt = optLong("createdAt", System.currentTimeMillis()),
        updatedAt = optLong("updatedAt", System.currentTimeMillis())
    )
}

private fun Product.toJson(): JSONObject {
    val aliasesArray = JSONArray()
    aliases.forEach { aliasesArray.put(it.toJson()) }
    return JSONObject().apply {
        put("id", id)
        put("name", name)
        put("aliases", aliasesArray)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }
}
