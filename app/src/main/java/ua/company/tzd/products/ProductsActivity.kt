package ua.company.tzd.products

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ua.company.tzd.databinding.ActivityProductsBinding

/**
 * Початковий варіант екрану "Товари".
 * Поки що тут тільки текст-заглушка, але структура вже готова для майбутніх списків.
 */
class ProductsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(ua.company.tzd.R.string.products_title)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
}
