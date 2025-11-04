package ua.company.tzd

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import ua.company.tzd.databinding.ActivityMainBinding
import ua.company.tzd.model.ScannedItem
import ua.company.tzd.settings.ParserConfig
import ua.company.tzd.settings.SettingsRepository
import ua.company.tzd.ui.ScannedAdapter
import ua.company.tzd.ui.SummaryAdapter
import ua.company.tzd.util.ParserUtil
import ua.company.tzd.util.TotalsUtil

/**
 * Основний екран для сканування кодів та перегляду поточних підсумків.
 * Коментарі пояснюють кожен крок, щоб навіть новачок розібрався в логіці.
 */
class MainActivity : AppCompatActivity() {

    // ViewBinding надає доступ до елементів розмітки без пошуку за ідентифікатором.
    private lateinit var binding: ActivityMainBinding
    // Репозиторій налаштувань зберігає позиції для парсингу та прапорець підтвердження.
    private lateinit var settingsRepository: SettingsRepository
    // Основний список усіх відсканованих кодів (нові елементи додаємо на початок).
    private val scannedItems: MutableList<ScannedItem> = mutableListOf()
    // Адаптери для двох списків у верхній частині екрана.
    private val summaryAdapter = SummaryAdapter()
    private val scannedAdapter = ScannedAdapter { item -> handleDeleteRequest(item) }
    // Поточна конфігурація парсера зчитується із DataStore й кешується тут.
    private var parserConfig: ParserConfig = SettingsRepository.DEFAULT_PARSER
    // Чи потрібно показувати діалог підтвердження під час видалення елемента.
    private var confirmDelete: Boolean = SettingsRepository.DEFAULT_CONFIRM_DELETE

    // Лончер для запуску ScanActivity та отримання результату.
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

        settingsRepository = SettingsRepository(applicationContext)

        // Налаштовуємо списки та одразу показуємо стан «порожньо».
        setupRecyclerViews()
        // Читаємо налаштування з DataStore та реагуємо на їх зміни у реальному часі.
        observeSettings()

        // Кнопка «Сканувати» запускає типовий процес з перевіркою дозволу.
        binding.btnScan.setOnClickListener {
            startScanFlow()
        }
        // Кнопка «Очистити» пропонує підтвердити повне очищення списку.
        binding.btnClear.setOnClickListener {
            showClearAllDialog()
        }
    }

    private fun startScanFlow() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                // Якщо дозвіл уже надано, просто відкриваємо сканер.
                openScanner()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Пояснюємо користувачу, навіщо дозвіл, і повторюємо запит.
                Toast.makeText(this, R.string.permission_camera_rationale, Toast.LENGTH_LONG).show()
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                // Вперше запитуємо дозвіл без попередніх пояснень.
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openScanner() {
        val intent = Intent(this, ScanActivity::class.java)
        scanLauncher.launch(intent)
    }

    private fun setupRecyclerViews() {
        // Горизонтальні списки не потрібні, тому обираємо звичайний LinearLayoutManager.
        binding.rvSummaryByArticle.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = summaryAdapter
        }
        binding.rvCodes.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = scannedAdapter
        }
        binding.tvGrandTotal.text = getString(R.string.grand_total_fmt, 0, 0, 0)
        updateEmptyState()
    }

    private fun observeSettings() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                settingsRepository.settingsFlow.collect { state ->
                    // Оновлюємо локальні змінні, щоб у потрібний момент застосувати нові параметри.
                    parserConfig = state.parserConfig
                    confirmDelete = state.confirmDelete
                }
            }
        }
    }

    private fun processScannedCode(code: String) {
        try {
            // Використовуємо налаштування користувача для розрізання коду на артикул та вагу.
            val (article, kg, g) = ParserUtil.extractArticleKgG(code, parserConfig)
            val item = ScannedItem(
                code = code,
                article = article,
                kg = kg,
                g = g,
                time = System.currentTimeMillis()
            )
            // Новий запис розміщуємо на початку списку.
            scannedItems.add(0, item)
            scannedAdapter.submitList(scannedItems.toList())
            binding.rvCodes.scrollToPosition(0)
            recalculateSummary()
        } catch (ex: IllegalArgumentException) {
            Toast.makeText(this, R.string.error_no_barcode, Toast.LENGTH_SHORT).show()
        }
    }

    private fun recalculateSummary() {
        // Групуємо коди за артикулом та одразу нормалізуємо вагу.
        val summary = TotalsUtil.groupByArticle(scannedItems)
        summaryAdapter.submitList(summary)
        val (kg, g) = TotalsUtil.calcGrandTotal(summary)
        val totalCount = scannedItems.size
        binding.tvGrandTotal.text = getString(R.string.grand_total_fmt, totalCount, kg, g)
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val hasItems = scannedItems.isNotEmpty()
        binding.summaryHeader.visibility = if (hasItems) View.VISIBLE else View.GONE
        binding.rvSummaryByArticle.visibility = if (hasItems) View.VISIBLE else View.GONE
        binding.tvGrandTotal.visibility = if (hasItems) View.VISIBLE else View.GONE
        binding.rvCodes.visibility = if (hasItems) View.VISIBLE else View.GONE
        binding.tvEmpty.visibility = if (hasItems) View.GONE else View.VISIBLE
    }

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

    private fun removeItem(item: ScannedItem) {
        // Видаляємо запис і оновлюємо статистику. Метод submitList створює копію, щоб adapter оновився.
        scannedItems.remove(item)
        scannedAdapter.submitList(scannedItems.toList())
        recalculateSummary()
    }

    private fun showClearAllDialog() {
        if (scannedItems.isEmpty()) {
            // Якщо список і так порожній, не показуємо діалог і просто завершуємо метод.
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_all_title)
            .setMessage(R.string.clear_all_msg)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                // Після підтвердження повністю очищаємо список та оновлюємо інтерфейс.
                scannedItems.clear()
                scannedAdapter.submitList(scannedItems.toList())
                recalculateSummary()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}
