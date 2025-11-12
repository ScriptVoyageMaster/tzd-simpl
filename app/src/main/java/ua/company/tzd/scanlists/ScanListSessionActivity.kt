package ua.company.tzd.scanlists

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ua.company.tzd.R
import ua.company.tzd.ScanActivity
import ua.company.tzd.databinding.ActivityMainBinding
import ua.company.tzd.localization.LocalizedActivity
import ua.company.tzd.model.ScannedItem
import ua.company.tzd.settings.ParserConfig
import ua.company.tzd.settings.SettingsRepository
import ua.company.tzd.ui.ScannedAdapter
import ua.company.tzd.ui.SummaryAdapter
import ua.company.tzd.util.ParserUtil
import ua.company.tzd.util.TotalsUtil

/**
 * Екран повторює стандартний режим сканування, але всі дані одразу пишуться в обраний список.
 */
class ScanListSessionActivity : LocalizedActivity() {

    companion object {
        const val EXTRA_LIST_ID = "ua.company.tzd.scanlists.EXTRA_LIST_ID"
        const val EXTRA_LIST_NAME = "ua.company.tzd.scanlists.EXTRA_LIST_NAME"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var scanListsRepository: ScanListsRepository

    private val scannedItems: MutableList<ScannedItem> = mutableListOf()
    private val summaryAdapter = SummaryAdapter()
    private val scannedAdapter = ScannedAdapter { item -> handleDeleteRequest(item) }

    private var parserConfig: ParserConfig = SettingsRepository.DEFAULT_PARSER
    private var confirmDelete: Boolean = SettingsRepository.DEFAULT_CONFIRM_DELETE
    private var allowedPrefixes: Set<String> = SettingsRepository.DEFAULT_ALLOWED_PREFIXES
    private val prefixComparator = compareBy<String> { it.length }.thenBy { it }

    private var listId: String? = null
    private var saveJob: Job? = null

    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val code = result.data?.getStringExtra(ScanActivity.EXTRA_BARCODE)
            if (!code.isNullOrEmpty()) {
                processScannedCode(code)
            } else {
                Toast.makeText(this, R.string.error_no_barcode, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            openScanner()
        } else {
            Toast.makeText(this, R.string.error_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val incomingListId = intent.getStringExtra(EXTRA_LIST_ID)
        if (incomingListId.isNullOrBlank()) {
            showListMissingAndFinish()
            return
        }
        listId = incomingListId

        intent.getStringExtra(EXTRA_LIST_NAME)?.let { initialName ->
            if (initialName.isNotBlank()) {
                title = getString(R.string.scan_list_session_title, initialName)
            }
        }

        settingsRepository = SettingsRepository(applicationContext)
        scanListsRepository = ScanListsRepository(applicationContext)

        setupRecyclerViews()
        observeSettings()
        loadStoredItems()

        binding.btnScan.setOnClickListener {
            startScanFlow()
        }
        binding.btnClear.setOnClickListener {
            showClearAllDialog()
        }
    }

    /**
     * Прочитуємо дані списку та попередні скани; якщо запис зник, повертаємо користувача назад.
     */
    private fun loadStoredItems() {
        lifecycleScope.launch {
            val id = listId ?: return@launch
            val info = scanListsRepository.getListInfo(id)
            if (info == null) {
                showListMissingAndFinish()
                return@launch
            }
            title = getString(R.string.scan_list_session_title, info.name)
            val stored = scanListsRepository.loadItems(id)
            scannedItems.clear()
            scannedItems.addAll(stored)
            scannedAdapter.submitList(scannedItems.toList())
            recalculateSummary()
        }
    }

    /**
     * Готуємо списки для підсумків та детальних сканів, використовуючи ті самі адаптери, що й у тимчасовому режимі.
     */
    private fun setupRecyclerViews() {
        binding.rvSummaryByArticle.apply {
            layoutManager = LinearLayoutManager(this@ScanListSessionActivity)
            adapter = summaryAdapter
        }
        binding.rvCodes.apply {
            layoutManager = LinearLayoutManager(this@ScanListSessionActivity)
            adapter = scannedAdapter
        }
        binding.tvGrandTotal.text = getString(R.string.grand_total_fmt, 0, 0, 0)
        updateEmptyState()
    }

    /**
     * Слухаємо зміни налаштувань, щоб під час сканування використовувати актуальні параметри парсингу та підтвердження.
     */
    private fun observeSettings() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                settingsRepository.settingsFlow.collect { state ->
                    parserConfig = state.parserConfig
                    confirmDelete = state.confirmDelete
                    allowedPrefixes = state.allowedPrefixes
                }
            }
        }
    }

    /**
     * Перевіряємо дозвіл на камеру та запускаємо сканер, якщо все гаразд.
     */
    private fun startScanFlow() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                openScanner()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(this, R.string.permission_camera_rationale, Toast.LENGTH_LONG).show()
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Відкриваємо ScanActivity, яка повертає перший знайдений штрихкод.
     */
    private fun openScanner() {
        val intent = Intent(this, ScanActivity::class.java)
        scanLauncher.launch(intent)
    }

    /**
     * Обробляємо штрихкод: валідація, парсинг, додавання до списку та автозбереження.
     */
    private fun processScannedCode(code: String) {
        if (!ParserUtil.isValidEan13(code)) {
            Toast.makeText(this, R.string.error_no_barcode, Toast.LENGTH_SHORT).show()
            return
        }
        if (!isCodeAllowedByPrefixes(code)) {
            showInvalidPrefixDialog(code)
            return
        }
        try {
            val (article, kg, g) = ParserUtil.extractArticleKgG(code, parserConfig)
            val item = ScannedItem(
                code = code,
                article = article,
                kg = kg,
                g = g,
                time = System.currentTimeMillis()
            )
            scannedItems.add(0, item)
            scannedAdapter.submitList(scannedItems.toList())
            binding.rvCodes.scrollToPosition(0)
            recalculateSummary()
            persistItems()
        } catch (ex: IllegalArgumentException) {
            Toast.makeText(this, R.string.error_no_barcode, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Пояснюємо користувачу, чому код відхилено, і дозволяємо швидко повторити спробу.
     */
    private fun showInvalidPrefixDialog(code: String) {
        val sorted = allowedPrefixes.toList().sortedWith(prefixComparator)
        AlertDialog.Builder(this)
            .setTitle(R.string.invalid_prefix_title)
            .setMessage(getString(R.string.invalid_prefix_msg, code, sorted.joinToString(", ")))
            .setPositiveButton(R.string.action_rescan) { _, _ ->
                openScanner()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * Якщо список префіксів порожній, приймаємо всі коди, інакше шукаємо збіг на початку рядка.
     */
    private fun isCodeAllowedByPrefixes(code: String): Boolean {
        if (allowedPrefixes.isEmpty()) {
            return true
        }
        return allowedPrefixes.any { prefix -> code.startsWith(prefix) }
    }

    /**
     * Оновлюємо підсумкову таблицю й загальну статистику після кожної зміни списку.
     */
    private fun recalculateSummary() {
        val summary = TotalsUtil.groupByArticle(scannedItems)
        summaryAdapter.submitList(summary)
        val (kg, g) = TotalsUtil.calcGrandTotal(summary)
        val totalCount = scannedItems.size
        binding.tvGrandTotal.text = getString(R.string.grand_total_fmt, totalCount, kg, g)
        updateEmptyState()
    }

    /**
     * Керуємо видимістю секцій: порожній стан чи таблиці з даними.
     */
    private fun updateEmptyState() {
        val hasItems = scannedItems.isNotEmpty()
        binding.summaryHeader.visibility = if (hasItems) View.VISIBLE else View.GONE
        binding.rvSummaryByArticle.visibility = if (hasItems) View.VISIBLE else View.GONE
        binding.tvGrandTotal.visibility = if (hasItems) View.VISIBLE else View.GONE
        binding.rvCodes.visibility = if (hasItems) View.VISIBLE else View.GONE
        binding.tvEmpty.visibility = if (hasItems) View.GONE else View.VISIBLE
    }

    /**
     * За потреби показуємо підтвердження перед видаленням окремого скану.
     */
    private fun handleDeleteRequest(item: ScannedItem) {
        if (confirmDelete) {
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_item_title)
                .setMessage(getString(R.string.delete_item_msg, item.code))
                .setPositiveButton(R.string.action_delete) { _, _ ->
                    removeItem(item)
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        } else {
            removeItem(item)
        }
    }

    /**
     * Прибираємо елемент зі списку, оновлюємо UI та робимо автозбереження.
     */
    private fun removeItem(item: ScannedItem) {
        scannedItems.remove(item)
        scannedAdapter.submitList(scannedItems.toList())
        recalculateSummary()
        persistItems()
    }

    /**
     * Повністю очищаємо список після підтвердження, щоби не видалити дані випадково.
     */
    private fun showClearAllDialog() {
        if (scannedItems.isEmpty()) {
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_all_title)
            .setMessage(R.string.clear_all_msg)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                scannedItems.clear()
                scannedAdapter.submitList(scannedItems.toList())
                recalculateSummary()
                persistItems()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * Зберігаємо актуальний стан списку в DataStore, щоб при наступному відкритті нічого не загубилося.
     */
    private fun persistItems() {
        val id = listId ?: return
        val snapshot = scannedItems.toList()
        saveJob?.cancel()
        saveJob = lifecycleScope.launch {
            scanListsRepository.saveItems(id, snapshot)
        }
    }

    /**
     * Якщо списку немає, попереджаємо користувача і повертаємо на попередній екран.
     */
    private fun showListMissingAndFinish() {
        Toast.makeText(this, R.string.scan_lists_not_found, Toast.LENGTH_LONG).show()
        finish()
    }
}
