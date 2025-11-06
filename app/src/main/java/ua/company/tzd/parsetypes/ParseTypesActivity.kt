package ua.company.tzd.parsetypes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ua.company.tzd.databinding.ActivityParseTypesBinding

/**
 * Базовий каркас екрану "Види парсингу".
 * Надалі сюди буде додано повноцінний список та редактор схем.
 */
class ParseTypesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParseTypesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParseTypesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(ua.company.tzd.R.string.parse_types_title)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Виводимо текст заглушки через форматований ресурс, щоб плейсхолдер працював без помилок побудови.
        binding.tvPlaceholder.text = getString(
            ua.company.tzd.R.string.placeholder_wip,
            getString(ua.company.tzd.R.string.placeholder_wip_message)
        )
    }
}
