package ua.company.tzd

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ua.company.tzd.databinding.ActivityHomeBinding
import ua.company.tzd.lists.ScanListsActivity
import ua.company.tzd.parsetypes.ParseTypesActivity
import ua.company.tzd.products.ProductsActivity
import ua.company.tzd.settings.UiSettingsManager

/**
 * Оновлене головне меню з чотирма розділами: списки, товари, види парсингу та налаштування.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val uiSettingsManager: UiSettingsManager by lazy { (application as TzdApp).uiSettingsManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Гарантуємо, що тема та мова застосовані ще до створення інтерфейсу.
        uiSettingsManager.ensureInitialized()

        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = getString(R.string.home_title)

        binding.btnGoScanLists.setOnClickListener {
            startActivity(Intent(this, ScanListsActivity::class.java))
        }
        binding.btnGoProducts.setOnClickListener {
            startActivity(Intent(this, ProductsActivity::class.java))
        }
        binding.btnGoParseTypes.setOnClickListener {
            startActivity(Intent(this, ParseTypesActivity::class.java))
        }
        binding.btnGoSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
