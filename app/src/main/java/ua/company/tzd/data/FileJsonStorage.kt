package ua.company.tzd.data

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * Простий сервіс для читання та запису JSON-файлів у приватну директорію застосунку.
 * Реалізує атомарний запис: спочатку зберігаємо у тимчасовий файл, потім замінюємо оригінал.
 * Такий підхід захищає від втрати даних у разі збою під час збереження.
 */
class FileJsonStorage(
    private val context: Context
) {

    /**
     * Зчитуємо вміст файлу як рядок. Якщо файлу не існує — повертаємо null, бо дані ще не створені.
     */
    fun readOrNull(fileName: String): String? {
        val file = targetFile(fileName)
        if (!file.exists()) {
            return null
        }
        return try {
            file.readText()
        } catch (error: IOException) {
            // Якщо сталось пошкодження, повертаємо null, щоб вище рівні могли створити дефолтні дані.
            null
        }
    }

    /**
     * Записуємо переданий JSON-рядок у файл із гарантією атомарності.
     */
    fun writeAtomic(fileName: String, json: String) {
        val file = targetFile(fileName)
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(json)
        if (!temp.renameTo(file)) {
            // Якщо з першої спроби перейменувати не вдалося, пробуємо видалити старий файл та повторити.
            file.delete()
            if (!temp.renameTo(file)) {
                throw IOException("Не вдалося зберегти файл $fileName")
            }
        }
    }

    /**
     * Видаляємо файл, якщо він існує. Використовується при стиранні журналу або списку.
     */
    fun delete(fileName: String) {
        targetFile(fileName).takeIf { it.exists() }?.delete()
    }

    private fun targetFile(fileName: String): File {
        val dir = context.filesDir
        return File(dir, fileName)
    }
}
