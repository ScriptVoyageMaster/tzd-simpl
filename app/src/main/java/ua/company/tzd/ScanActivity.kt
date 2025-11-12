package ua.company.tzd

import android.content.Intent
import android.os.Bundle
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompoundBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import ua.company.tzd.databinding.ActivityScanBinding
import ua.company.tzd.localization.LocalizedActivity

/**
 * Екран сканування, що використовує бібліотеку ZXing Embedded для швидкого пошуку EAN-13.
 * Детальні коментарі допомагають зрозуміти послідовність дій у коді.
 */
class ScanActivity : LocalizedActivity(), DecoratedBarcodeView.TorchListener {

    companion object {
        const val EXTRA_BARCODE = "ua.company.tzd.EXTRA_BARCODE"
    }

    // ViewBinding для доступу до елементів розмітки без зайвого коду.
    private lateinit var binding: ActivityScanBinding

    // Прапорець, щоб не відправляти результат декілька разів.
    private var isResultDelivered = false

    // Зберігаємо стан ліхтарика, щоб синхронізувати кнопку.
    private var isTorchEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Створюємо binding і показуємо розмітку зі сканером.
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Прибираємо стандартний текст статусу, щоб не відволікати користувача.
        binding.barcodeScanner.setStatusText("")

        // Обмежуємо формати сканування лише до EAN_13, щоб уникнути сторонніх результатів.
        val formats = listOf(com.google.zxing.BarcodeFormat.EAN_13)
        binding.barcodeScanner.barcodeView.decoderFactory = DefaultDecoderFactory(formats)
        binding.barcodeScanner.setTorchListener(this)

        // Оновлюємо підказку та підпис кнопки з ресурсів для локалізації.
        binding.tvHint.text = getString(R.string.hint_hold_steady)
        updateTorchUi()

        // Запускаємо одноразове сканування: перший успішний результат одразу повертається.
        binding.barcodeScanner.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                if (result == null || isResultDelivered) return
                // Позначаємо, що результат оброблено, щоб уникнути повторів.
                isResultDelivered = true
                deliverResult(result.text)
            }

            override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {
                // Для простоти не обробляємо підказки точок, але метод треба перевизначити.
            }
        })

        // Кнопка ліхтарика дає змогу вмикати та вимикати підсвітку під час сканування.
        binding.btnTorch.setOnClickListener {
            toggleTorch()
        }
    }

    override fun onResume() {
        super.onResume()
        // Відновлюємо попередній перегляд камери при поверненні на екран.
        binding.barcodeScanner.resume()
    }

    override fun onPause() {
        // Призупиняємо камеру, щоб звільнити ресурси, коли Activity неактивна.
        binding.barcodeScanner.pause()
        super.onPause()
    }

    override fun onTorchOn() {
        // Записуємо новий стан, щойно ZXing підтверджує ввімкнення ліхтарика.
        isTorchEnabled = true
        updateTorchUi()
    }

    override fun onTorchOff() {
        // Аналогічно фіксуємо вимкнення ліхтарика.
        isTorchEnabled = false
        updateTorchUi()
    }

    private fun toggleTorch() {
        // Перемикаємо стан ліхтарика, якщо пристрій підтримує цю функцію.
        val barcodeView: CompoundBarcodeView = binding.barcodeScanner
        if (isTorchEnabled) {
            barcodeView.setTorchOff()
        } else {
            barcodeView.setTorchOn()
        }
    }

    private fun updateTorchUi() {
        // Встановлюємо текст та опис кнопки відповідно до поточного стану ліхтарика.
        if (isTorchEnabled) {
            binding.btnTorch.text = getString(R.string.torch_on)
            binding.btnTorch.contentDescription = getString(R.string.torch_on)
        } else {
            binding.btnTorch.text = getString(R.string.torch_off)
            binding.btnTorch.contentDescription = getString(R.string.torch_off)
        }
    }

    private fun deliverResult(value: String) {
        // Готуємо відповідь для головної Activity і закриваємо екран сканування.
        val data = Intent().putExtra(EXTRA_BARCODE, value)
        setResult(RESULT_OK, data)
        finish()
    }
}
