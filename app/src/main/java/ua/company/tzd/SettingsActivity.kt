package ua.company.tzd

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import ua.company.tzd.databinding.ActivitySettingsBinding
import ua.company.tzd.localization.LocalizedActivity
import ua.company.tzd.localization.LocaleManager
import ua.company.tzd.settings.ParserConfig
import ua.company.tzd.settings.SettingsRepository
import ua.company.tzd.settings.SettingsValidator
import ua.company.tzd.util.ParserUtil

/**
 * Екран налаштувань дозволяє змінити правила парсингу та поведінку підтвердження видалення.
 */
class SettingsActivity : LocalizedActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repository: SettingsRepository
    private val currentPrefixes: MutableList<String> = mutableListOf()
    private val prefixComparator = compareBy<String> { it.length }.thenBy { it }
    private val prefixRegex = Regex("\\d{1,13}")
    private var suppressLanguageListener = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = SettingsRepository(applicationContext)

        // Встановлюємо заголовок активності відповідно до локалізованого ресурсу.
        title = getString(R.string.settings_title)

        // Заздалегідь очищаємо помилки при зміні тексту, щоб користувач бачив актуальний стан.
        listOf(
            binding.tilArticleStart,
            binding.tilArticleLength,
            binding.tilKgStart,
            binding.tilKgLength,
            binding.tilGStart,
            binding.tilGLength,
            binding.tilPrefix
        ).forEach { layout ->
            layout.editText?.addTextChangedListener { layout.error = null }
        }

        setupLanguageSection()
        setupPrefixSection()
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
                    updateUi(state.parserConfig, state.confirmDelete, state.allowedPrefixes)
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
     * Готуємо блок роботи з префіксами: додавання, очищення помилок та обробку натискань.
     */
    private fun setupPrefixSection() {
        binding.btnAddPrefix.setOnClickListener { addPrefixFromInput() }
        binding.tilPrefix.editText?.setOnEditorActionListener { _, _, _ ->
            addPrefixFromInput()
            true
        }
    }

    /**
     * Готуємо перемикач мови: слухаємо вибір користувача та реагуємо на зміни у DataStore.
     */
    private fun setupLanguageSection() {
        binding.rgLanguage.setOnCheckedChangeListener { _, checkedId ->
            if (suppressLanguageListener) {
                return@setOnCheckedChangeListener
            }
            val selectedLanguage = when (checkedId) {
                binding.rbLanguageUk.id -> LocaleManager.LANGUAGE_UK
                binding.rbLanguageEn.id -> LocaleManager.LANGUAGE_EN
                else -> return@setOnCheckedChangeListener
            }
            lifecycleScope.launch {
                val current = LocaleManager.getLanguage(applicationContext)
                if (current != selectedLanguage) {
                    // Зберігаємо нову мову та одразу перезавантажуємо екран, щоб тексти оновилися миттєво.
                    LocaleManager.setLanguage(applicationContext, selectedLanguage)
                    recreate()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                LocaleManager.languageFlow(applicationContext).collect { language ->
                    val targetId = when (language) {
                        LocaleManager.LANGUAGE_EN -> binding.rbLanguageEn.id
                        else -> binding.rbLanguageUk.id
                    }
                    if (binding.rgLanguage.checkedRadioButtonId != targetId) {
                        suppressLanguageListener = true
                        binding.rgLanguage.check(targetId)
                        suppressLanguageListener = false
                    }
                }
            }
        }
    }

    /**
     * Заповнюємо інтерфейс значеннями, що прийшли з репозиторію.
     */
    private fun updateUi(config: ParserConfig, confirmDelete: Boolean, prefixes: Set<String>) {
        // Щоб не переривати введення користувача, оновлюємо поле лише коли воно не в фокусі.
        updateField(binding.tilArticleStart, config.articleStart)
        updateField(binding.tilArticleLength, config.articleLength)
        updateField(binding.tilKgStart, config.kgStart)
        updateField(binding.tilKgLength, config.kgLength)
        updateField(binding.tilGStart, config.gStart)
        updateField(binding.tilGLength, config.gLength)
        binding.swConfirmDelete.isChecked = confirmDelete
        renderPrefixes(prefixes)
        binding.tvTestPrefixMessage.visibility = View.GONE
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
     * Оновлюємо розділ з префіксами після отримання даних із DataStore.
     */
    private fun renderPrefixes(prefixes: Set<String>) {
        currentPrefixes.clear()
        currentPrefixes.addAll(prefixes.sortedWith(prefixComparator))
        refreshPrefixChips()
        binding.tilPrefix.error = null
        binding.etPrefix.text?.clear()
    }

    /**
     * Створюємо чіпи для кожного префікса та показуємо інформаційне повідомлення, якщо список порожній.
     */
    private fun refreshPrefixChips() {
        binding.chipGroupPrefixes.removeAllViews()
        currentPrefixes.forEach { prefix ->
            binding.chipGroupPrefixes.addView(createPrefixChip(prefix))
        }
        binding.tvPrefixesEmpty.visibility = if (currentPrefixes.isEmpty()) View.VISIBLE else View.GONE
    }

    /**
     * Формуємо чіп із префіксом та навішуємо обробники на торкання та іконку видалення.
     */
    private fun createPrefixChip(prefix: String): Chip {
        return Chip(this).apply {
            text = prefix
            isCloseIconVisible = true
            isCheckable = false
            setOnCloseIconClickListener { confirmRemovePrefix(prefix) }
            setOnClickListener { confirmRemovePrefix(prefix) }
        }
    }

    /**
     * Після підтвердження видаляємо префікс зі списку й оновлюємо чіпи на екрані.
     */
    private fun confirmRemovePrefix(prefix: String) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.settings_prefix_remove, prefix))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                currentPrefixes.remove(prefix)
                refreshPrefixChips()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * Зчитуємо введене значення, нормалізуємо його та додаємо до списку, якщо пройшло валідацію.
     */
    private fun addPrefixFromInput() {
        val raw = binding.etPrefix.text?.toString().orEmpty()
        val compact = raw.filterNot { it.isWhitespace() }
        if (compact.isEmpty() || !compact.matches(prefixRegex)) {
            binding.tilPrefix.error = getString(R.string.settings_prefix_invalid)
            return
        }
        if (currentPrefixes.contains(compact)) {
            binding.tilPrefix.error = getString(R.string.settings_prefix_exists)
            return
        }
        currentPrefixes.add(compact)
        currentPrefixes.sortWith(prefixComparator)
        binding.etPrefix.text?.clear()
        binding.tilPrefix.error = null
        refreshPrefixChips()
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
            repository.saveSettings(config, binding.swConfirmDelete.isChecked, currentPrefixes.toSet())
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
            binding.tilGLength,
            binding.tilPrefix
        ).forEach { it.error = null }
        binding.tvTestPrefixMessage.visibility = View.GONE
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
        if (!ParserUtil.isValidEan13(code)) {
            Toast.makeText(this, R.string.error_no_barcode, Toast.LENGTH_SHORT).show()
            return
        }
        if (!isCodeAllowedByPrefixes(code)) {
            val sorted = currentPrefixes.sortedWith(prefixComparator)
            binding.tvTestPrefixMessage.text = getString(R.string.settings_prefix_not_allowed, code, sorted.joinToString(", "))
            binding.tvTestPrefixMessage.visibility = View.VISIBLE
            return
        }
        try {
            val (article, kg, g) = ParserUtil.extractArticleKgG(code, config)
            val message = getString(R.string.test_code_result_fmt, article, kg, g)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        } catch (ex: IllegalArgumentException) {
            Toast.makeText(this, R.string.test_code_error, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Перевіряємо фільтр префіксів для тестового коду: порожній список означає «дозволити все».
     */
    private fun isCodeAllowedByPrefixes(code: String): Boolean {
        if (currentPrefixes.isEmpty()) {
            return true
        }
        return currentPrefixes.any { prefix -> code.startsWith(prefix) }
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
