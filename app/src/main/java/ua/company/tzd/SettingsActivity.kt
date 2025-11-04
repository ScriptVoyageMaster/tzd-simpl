package ua.company.tzd

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import ua.company.tzd.databinding.ActivitySettingsBinding
import ua.company.tzd.settings.ParserConfig
import ua.company.tzd.settings.SettingsRepository
import ua.company.tzd.settings.SettingsValidator
import ua.company.tzd.util.ParserUtil

/**
 * Екран налаштувань дозволяє змінити правила парсингу та поведінку підтвердження видалення.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = SettingsRepository(applicationContext)

        // Заздалегідь очищаємо помилки при зміні тексту, щоб користувач бачив актуальний стан.
        listOf(
            binding.tilArticleStart,
            binding.tilArticleLength,
            binding.tilKgStart,
            binding.tilKgLength,
            binding.tilGStart,
            binding.tilGLength
        ).forEach { layout ->
            layout.editText?.addTextChangedListener { layout.error = null }
        }

        observeSettings()
        setupButtons()
    }

    /**
     * Підписуємося на оновлення DataStore й оновлюємо поля форми.
     */
    private fun observeSettings() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                repository.settingsFlow.collect { state ->
                    updateUi(state.parserConfig, state.confirmDelete)
                }
            }
        }
    }

    /**
     * Прив'язуємо обробники до кнопок збереження, скидання та тестового розбору.
     */
    private fun setupButtons() {
        binding.btnSave.setOnClickListener { saveSettings() }
        binding.btnResetDefaults.setOnClickListener {
            lifecycleScope.launch { repository.resetDefaults() }
        }
        binding.btnTest.setOnClickListener { runTestParsing() }
    }

    /**
     * Заповнюємо інтерфейс значеннями, що прийшли з репозиторію.
     */
    private fun updateUi(config: ParserConfig, confirmDelete: Boolean) {
        // Щоб не переривати введення користувача, оновлюємо поле лише коли воно не в фокусі.
        updateField(binding.tilArticleStart, config.articleStart)
        updateField(binding.tilArticleLength, config.articleLength)
        updateField(binding.tilKgStart, config.kgStart)
        updateField(binding.tilKgLength, config.kgLength)
        updateField(binding.tilGStart, config.gStart)
        updateField(binding.tilGLength, config.gLength)
        binding.swConfirmDelete.isChecked = confirmDelete
    }

    private fun updateField(layout: TextInputLayout, value: Int) {
        val edit = layout.editText ?: return
        if (!edit.hasFocus()) {
            val newText = value.toString()
            if (edit.text?.toString() != newText) {
                edit.setText(newText)
            }
        }
    }

    /**
     * Зчитуємо значення з форми, проводимо валідацію та зберігаємо у DataStore.
     */
    private fun saveSettings() {
        clearErrors()
        val config = ParserConfig(
            articleStart = binding.tilArticleStart.parseIntOrError(),
            articleLength = binding.tilArticleLength.parseIntOrError(),
            kgStart = binding.tilKgStart.parseIntOrError(),
            kgLength = binding.tilKgLength.parseIntOrError(),
            gStart = binding.tilGStart.parseIntOrError(),
            gLength = binding.tilGLength.parseIntOrError()
        )

        if (config.containsNulls()) {
            return
        }

        if (markRangeErrors(config)) {
            Toast.makeText(this, R.string.error_out_of_bounds, Toast.LENGTH_LONG).show()
            return
        }

        if (!SettingsValidator.isValid(config)) {
            showOverlapErrors()
            Toast.makeText(this, R.string.error_ranges_overlap, Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            repository.saveSettings(config, binding.swConfirmDelete.isChecked)
            Toast.makeText(this@SettingsActivity, R.string.saved_ok, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Пояснюємо користувачу, які поля спричинили помилку, щоб він міг швидко виправити значення.
     */
    private fun showOverlapErrors() {
        val resources = listOf(
            binding.tilArticleStart,
            binding.tilArticleLength,
            binding.tilKgStart,
            binding.tilKgLength,
            binding.tilGStart,
            binding.tilGLength
        )
        resources.forEach { layout ->
            if (layout.error.isNullOrEmpty()) {
                layout.error = getString(R.string.error_ranges_overlap)
            }
        }
    }

    private fun clearErrors() {
        listOf(
            binding.tilArticleStart,
            binding.tilArticleLength,
            binding.tilKgStart,
            binding.tilKgLength,
            binding.tilGStart,
            binding.tilGLength
        ).forEach { it.error = null }
    }

    /**
     * Перевіряємо, чи не виходять діапазони за межі штрих-коду, і підсвічуємо помилки.
     */
    private fun markRangeErrors(config: ParserConfig): Boolean {
        var hasError = false
        if (!SettingsValidator.isRangeValid(config.articleStart, config.articleLength)) {
            binding.tilArticleStart.error = getString(R.string.error_out_of_bounds)
            binding.tilArticleLength.error = getString(R.string.error_out_of_bounds)
            hasError = true
        }
        if (!SettingsValidator.isRangeValid(config.kgStart, config.kgLength)) {
            binding.tilKgStart.error = getString(R.string.error_out_of_bounds)
            binding.tilKgLength.error = getString(R.string.error_out_of_bounds)
            hasError = true
        }
        if (!SettingsValidator.isRangeValid(config.gStart, config.gLength)) {
            binding.tilGStart.error = getString(R.string.error_out_of_bounds)
            binding.tilGLength.error = getString(R.string.error_out_of_bounds)
            hasError = true
        }
        return hasError
    }

    /**
     * Дозволяємо швидко перевірити, як саме буде розібрано тестовий код.
     */
    private fun runTestParsing() {
        clearErrors()
        val code = binding.etTestCode.text?.toString().orEmpty()
        val config = ParserConfig(
            articleStart = binding.tilArticleStart.parseIntOrError(),
            articleLength = binding.tilArticleLength.parseIntOrError(),
            kgStart = binding.tilKgStart.parseIntOrError(),
            kgLength = binding.tilKgLength.parseIntOrError(),
            gStart = binding.tilGStart.parseIntOrError(),
            gLength = binding.tilGLength.parseIntOrError()
        )
        if (config.containsNulls()) {
            Toast.makeText(this, R.string.error_out_of_bounds, Toast.LENGTH_SHORT).show()
            return
        }
        if (markRangeErrors(config)) {
            Toast.makeText(this, R.string.error_out_of_bounds, Toast.LENGTH_SHORT).show()
            return
        }
        if (!SettingsValidator.isValid(config)) {
            Toast.makeText(this, R.string.error_ranges_overlap, Toast.LENGTH_SHORT).show()
            return
        }
        if (code.isEmpty()) {
            Toast.makeText(this, R.string.test_code_empty, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val (article, kg, g) = ParserUtil.extractArticleKgG(code, config)
            val message = getString(R.string.test_code_result_fmt, article, kg, g)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        } catch (ex: IllegalArgumentException) {
            Toast.makeText(this, getString(R.string.test_code_error, ex.message), Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Розширення для зручного читання числа з TextInputLayout.
     */
    private fun TextInputLayout.parseIntOrError(): Int {
        val text = editText?.text?.toString()?.trim().orEmpty()
        val number = text.toIntOrNull()
        if (number == null) {
            error = getString(R.string.error_number_required)
            return NULL_MARKER
        }
        return number
    }

    private fun ParserConfig.containsNulls(): Boolean {
        return listOf(articleStart, articleLength, kgStart, kgLength, gStart, gLength).any { it == NULL_MARKER }
    }

    companion object {
        private const val NULL_MARKER = -1
    }
}
