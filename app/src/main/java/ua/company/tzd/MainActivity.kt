package ua.company.tzd

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import ua.company.tzd.databinding.ActivityMainBinding

/**
 * Головний екран застосунку збирає штрих-коди та показує просту статистику.
 * Докладні коментарі пояснюють кроки для розробників-початківців.
 */
class MainActivity : AppCompatActivity() {

    // ViewBinding допомагає легко взаємодіяти з елементами макету без findViewById.
    private lateinit var binding: ActivityMainBinding

    // lastCode зберігає останній валідний штрих-код.
    private var lastCode: String? = null

    // Лічильник totalScanned показує, скільки кодів зчитано за поточну сесію.
    private var totalScanned: Int = 0

    // ActivityResultLauncher зручно отримує результат зі ScanActivity.
    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val code = result.data?.getStringExtra(ScanActivity.EXTRA_BARCODE)
            if (!code.isNullOrEmpty() && isValidEan13(code)) {
                // Приймаємо лише валідний EAN-13 та оновлюємо статистику.
                lastCode = code
                totalScanned += 1
                updateUi()
            } else {
                // Повідомляємо користувача про помилку, якщо код некоректний.
                Toast.makeText(this, R.string.error_no_barcode, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Лончер для запиту дозволу на камеру.
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            openScanner()
        } else {
            Toast.makeText(this, R.string.error_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ініціалізуємо binding, щоб отримати доступ до розмітки activity_main.xml.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Заповнюємо початкові значення текстових полів.
        binding.tvTitle.text = getString(R.string.title_main)
        updateUi()

        // Кнопка "Сканувати" відкриває ScanActivity після перевірки дозволів.
        binding.btnScan.setOnClickListener {
            startScanFlow()
        }

        // Кнопка "Очистити" скидає лічильники та текст.
        binding.btnClear.setOnClickListener {
            resetCounters()
        }
    }

    private fun startScanFlow() {
        // Перевіряємо, чи вже є дозвіл на камеру.
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                openScanner()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Пояснюємо причину та одразу запитуємо дозвіл.
                Toast.makeText(this, R.string.permission_camera_rationale, Toast.LENGTH_LONG).show()
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                // Запитуємо дозвіл без додаткових пояснень.
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openScanner() {
        // Формуємо Intent для запуску ScanActivity та очікуємо результат.
        val intent = Intent(this, ScanActivity::class.java)
        scanLauncher.launch(intent)
    }

    private fun resetCounters() {
        // Обнуляємо збережені дані та оновлюємо інтерфейс.
        lastCode = null
        totalScanned = 0
        updateUi()
    }

    private fun updateUi() {
        // Показуємо останній код або дефіс, якщо ще нічого не відскановано.
        val lastCodeText = lastCode ?: "—"
        binding.tvLastCode.text = "${getString(R.string.label_last_code)} $lastCodeText"

        // Відображаємо кількість успішних зчитувань.
        binding.tvTotal.text = "${getString(R.string.label_total_scanned)} $totalScanned"
    }

    private fun isValidEan13(code: String): Boolean {
        // Перевіряємо довжину та що всі символи є цифрами.
        if (code.length != 13 || code.any { !it.isDigit() }) {
            return false
        }

        // Конвертуємо символи у список цифр для математичних обчислень.
        val digits = code.map { it.digitToInt() }

        // Рахуємо суму за правилами EAN-13: парні позиції множимо на 3.
        var sum = 0
        for (index in 0 until 12) {
            val digit = digits[index]
            sum += if ((index + 1) % 2 == 0) digit * 3 else digit
        }

        // Обчислюємо контрольну цифру й порівнюємо з останньою.
        val checkDigit = (10 - (sum % 10)) % 10
        return checkDigit == digits[12]
    }
}
