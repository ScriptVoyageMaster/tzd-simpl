package ua.company.tzd

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ua.company.tzd.databinding.ActivityHomeBinding

/**
 * Проста стартова активність показує три великі кнопки для переходу на потрібний екран.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Встановлюємо заголовок вікна, щоб користувач розумів, де перебуває.
        title = getString(R.string.home_title)

        // Підписуємо кнопки на відповідні переходи.
        binding.btnGoScan.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        binding.btnGoScanLists.setOnClickListener {
            // Відкриваємо новий екран керування списками сканування.
            startActivity(Intent(this, ScanListsActivity::class.java))
        }
        binding.btnGoSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
