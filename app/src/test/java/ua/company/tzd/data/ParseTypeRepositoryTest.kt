package ua.company.tzd.data

import android.content.Context
import android.test.mock.MockContext
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Перевіряємо, що репозиторій створює нову схему із валідними полями.
 */
class ParseTypeRepositoryTest {

    /**
     * Допоміжний контекст для тесту, який повертає тимчасову директорію замість реальної файлової системи застосунку.
     */
    private class RepositoryTestContext(private val tempDir: File) : MockContext() {
        // Коментар для початківця: застосунок звертається до applicationContext, тому повертаємо себе ж.
        override fun getApplicationContext(): Context = this

        // Коментар для початківця: файли зберігаються у filesDir, тож вказуємо тимчасове місце для тестів.
        override fun getFilesDir(): File = tempDir
    }

    @Test
    fun newParseType_returnsSingleGroupField() {
        // Коментар для початківця: створюємо тимчасову папку, щоб репозиторій міг працювати з файловою системою.
        val tempDir = Files.createTempDirectory("parse-type-repo-test").toFile()
        val context = RepositoryTestContext(tempDir)
        val repository = ParseTypeRepository(context)

        val result = repository.newParseType("Тестова схема")

        // Перевіряємо, що виняток не стався (якщо б стався, тест би не дійшов до цього місця).
        assertEquals("Очікуємо одне поле в списку", 1, result.fields.size)
        assertEquals("Єдине поле має бути групувальним", ParseFieldRole.GROUP, result.fields.single().role)
    }
}
