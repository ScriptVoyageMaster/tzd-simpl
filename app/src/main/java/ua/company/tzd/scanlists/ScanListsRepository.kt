package ua.company.tzd.scanlists

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import ua.company.tzd.model.ScanListInfo
import ua.company.tzd.model.ScannedItem
import java.util.UUID

private const val SCAN_LISTS_DATASTORE_NAME = "scan_lists"

/**
 * Репозиторій відповідає за зберігання метаданих списків і всіх відсканованих позицій.
 * Дані лежать у DataStore, тому з ними можна працювати у фоновому потоці без блокування UI.
 */
class ScanListsRepository(private val context: Context) {

    companion object {
        private val KEY_LISTS = stringPreferencesKey("lists")

        private fun listDataKey(listId: String) = stringPreferencesKey("list.$listId.items")
    }

    /**
     * Потік дозволяє екрану «Списки сканування» оперативно реагувати на зміни у сховищі.
     */
    val listsFlow: Flow<List<ScanListInfo>> = context.scanListsDataStore.data
        .map { prefs -> parseLists(prefs[KEY_LISTS]) }

    /**
     * Створюємо новий список із випадковим ідентифікатором і порожнім набором сканів.
     */
    suspend fun createList(name: String): ScanListInfo {
        val timestamp = System.currentTimeMillis()
        val info = ScanListInfo(
            id = UUID.randomUUID().toString(),
            name = name,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        context.scanListsDataStore.edit { prefs ->
            val current = parseLists(prefs[KEY_LISTS])
            val updated = mutableListOf(info).apply { addAll(current) }
            val sorted = updated.sortedByDescending { it.updatedAt }
            prefs[KEY_LISTS] = serializeLists(sorted)
            prefs[listDataKey(info.id)] = serializeItems(emptyList())
        }
        return info
    }

    /**
     * Видаляємо список та усі пов'язані з ним дані.
     */
    suspend fun deleteList(listId: String) {
        context.scanListsDataStore.edit { prefs ->
            val remaining = parseLists(prefs[KEY_LISTS])
                .filterNot { it.id == listId }
                .sortedByDescending { it.updatedAt }
            prefs[KEY_LISTS] = serializeLists(remaining)
            prefs.remove(listDataKey(listId))
        }
    }

    /**
     * Повертаємо актуальний опис списку або null, якщо запис зник.
     */
    suspend fun getListInfo(listId: String): ScanListInfo? {
        val prefs = context.scanListsDataStore.data.first()
        return parseLists(prefs[KEY_LISTS]).firstOrNull { it.id == listId }
    }

    /**
     * Читаємо всі раніше відскановані позиції, щоб відновити стан екрану.
     */
    suspend fun loadItems(listId: String): List<ScannedItem> {
        val prefs = context.scanListsDataStore.data.first()
        val raw = prefs[listDataKey(listId)]
        return parseItems(raw)
    }

    /**
     * Зберігаємо повний набір позицій та оновлюємо мітку часу, щоб показати нещодавні списки вище.
     */
    suspend fun saveItems(listId: String, items: List<ScannedItem>) {
        val timestamp = System.currentTimeMillis()
        context.scanListsDataStore.edit { prefs ->
            val lists = parseLists(prefs[KEY_LISTS])
            val updatedLists = lists.map { info ->
                if (info.id == listId) {
                    info.copy(updatedAt = timestamp)
                } else {
                    info
                }
            }.sortedByDescending { it.updatedAt }
            prefs[KEY_LISTS] = serializeLists(updatedLists)
            prefs[listDataKey(listId)] = serializeItems(items)
        }
    }

    /**
     * Перетворюємо JSON-рядок зі списком у безпечну колекцію моделей.
     */
    private fun parseLists(raw: String?): List<ScanListInfo> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    val id = obj.optString("id")
                    if (id.isBlank()) continue
                    val name = obj.optString("name")
                    val createdAt = obj.optLong("createdAt", 0L)
                    val updatedAt = obj.optLong("updatedAt", createdAt)
                    add(ScanListInfo(id, name, createdAt, updatedAt))
                }
            }
        } catch (error: JSONException) {
            emptyList()
        }
    }

    /**
     * Перетворюємо колекцію моделей у компактний JSON-рядок для DataStore.
     */
    private fun serializeLists(items: List<ScanListInfo>): String {
        val array = JSONArray()
        items.forEach { info ->
            val obj = JSONObject()
            obj.put("id", info.id)
            obj.put("name", info.name)
            obj.put("createdAt", info.createdAt)
            obj.put("updatedAt", info.updatedAt)
            array.put(obj)
        }
        return array.toString()
    }

    /**
     * Зчитуємо збережені позиції списку та відтворюємо ScannedItem.
     */
    private fun parseItems(raw: String?): List<ScannedItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    val code = obj.optString("code")
                    val article = obj.optString("article")
                    if (code.isBlank() || article.isBlank()) continue
                    val kg = obj.optInt("kg")
                    val g = obj.optInt("g")
                    val time = obj.optLong("time")
                    add(ScannedItem(code, article, kg, g, time))
                }
            }
        } catch (error: JSONException) {
            emptyList()
        }
    }

    /**
     * Серіалізуємо позиції в JSON, щоб швидко відновлювати список.
     */
    private fun serializeItems(items: List<ScannedItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("code", item.code)
            obj.put("article", item.article)
            obj.put("kg", item.kg)
            obj.put("g", item.g)
            obj.put("time", item.time)
            array.put(obj)
        }
        return array.toString()
    }
}

/**
 * Розширення створює окремий DataStore, щоб не змішувати налаштування зі списками.
 */
private val Context.scanListsDataStore by preferencesDataStore(name = SCAN_LISTS_DATASTORE_NAME)
