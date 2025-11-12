package ua.company.tzd

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputLayout
import androidx.core.widget.doAfterTextChanged
import kotlinx.coroutines.launch
import ua.company.tzd.databinding.ActivityScanListsBinding
import ua.company.tzd.databinding.DialogCreateScanListBinding
import ua.company.tzd.model.ScanListInfo
import ua.company.tzd.scanlists.ScanListSessionActivity
import ua.company.tzd.scanlists.ScanListsAdapter
import ua.company.tzd.scanlists.ScanListsRepository
import ua.company.tzd.localization.LocalizedActivity

/**
 * Екран керування списками зберігає та показує назви для довготривалих сесій сканування.
 * Тут можна створити новий список, видалити його та перейти до режиму сканування.
 */
class ScanListsActivity : LocalizedActivity() {

    private lateinit var binding: ActivityScanListsBinding
    private lateinit var repository: ScanListsRepository
    private lateinit var adapter: ScanListsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanListsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = getString(R.string.scan_lists_title)

        repository = ScanListsRepository(applicationContext)
        adapter = ScanListsAdapter(
            onOpen = { info -> openScanSession(info) },
            onDelete = { info -> confirmDelete(info) }
        )

        binding.rvLists.layoutManager = LinearLayoutManager(this)
        binding.rvLists.adapter = adapter

        binding.btnCreate.setOnClickListener { showCreateDialog() }

        observeLists()
    }

    /**
     * Підписуємося на DataStore, щоб реагувати на зміни й оновлювати UI без ручного перезавантаження.
     */
    private fun observeLists() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                repository.listsFlow.collect { lists ->
                    adapter.submitList(lists)
                    binding.tvEmpty.visibility = if (lists.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                }
            }
        }
    }

    /**
     * Діалог створення списку з базовою валідацією назви.
     */
    private fun showCreateDialog() {
        val dialogBinding = DialogCreateScanListBinding.inflate(layoutInflater)
        val inputLayout = dialogBinding.root as TextInputLayout
        inputLayout.error = null

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.scan_lists_create_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialogBinding.etName.doAfterTextChanged {
                inputLayout.error = null
            }
            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positive.setOnClickListener {
                val name = dialogBinding.etName.text?.toString()?.trim().orEmpty()
                if (validateName(name, inputLayout)) {
                    lifecycleScope.launch {
                        repository.createList(name)
                    }
                    dialog.dismiss()
                }
            }
            dialogBinding.etName.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    positive.performClick()
                    true
                } else {
                    false
                }
            }
        }

        dialog.show()
    }

    /**
     * Перевіряємо довжину рядка та показуємо підказку в полі введення.
     */
    private fun validateName(name: String, layout: TextInputLayout): Boolean {
        val isValid = name.length in 1..60
        layout.error = if (isValid) null else getString(R.string.scan_lists_name_error)
        return isValid
    }

    /**
     * Підтверджуємо видалення, бо операція незворотна і стирає усі скани.
     */
    private fun confirmDelete(info: ScanListInfo) {
        AlertDialog.Builder(this)
            .setTitle(R.string.scan_lists_delete_title)
            .setMessage(getString(R.string.scan_lists_delete_msg, info.name))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                lifecycleScope.launch {
                    repository.deleteList(info.id)
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * Переходимо в екран сканування для вибраного списку.
     */
    private fun openScanSession(info: ScanListInfo) {
        val intent = Intent(this, ScanListSessionActivity::class.java)
        intent.putExtra(ScanListSessionActivity.EXTRA_LIST_ID, info.id)
        intent.putExtra(ScanListSessionActivity.EXTRA_LIST_NAME, info.name)
        startActivity(intent)
    }
}
