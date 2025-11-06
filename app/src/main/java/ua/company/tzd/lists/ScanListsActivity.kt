package ua.company.tzd.lists

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ua.company.tzd.databinding.ActivityScanListsBinding

/**
 * Заготовка нового екрану "Списки сканування".
 * Поки що показуємо інформативний текст і створюємо основу для майбутньої логіки.
 */
class ScanListsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanListsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanListsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(ua.company.tzd.R.string.scan_lists_title)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
}
