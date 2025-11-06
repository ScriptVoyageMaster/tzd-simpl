package ua.company.tzd

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Тимчасова заглушка для старого екрана підсумків.
 * Основний функціонал буде перенесено до нового екрану деталей списку.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
