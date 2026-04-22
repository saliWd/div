package ch.widmedia.Eintrag

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ch.widmedia.Eintrag.databinding.ActivityAddEntryBinding
import java.text.SimpleDateFormat
import java.util.*

class AddEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEntryBinding
    private val db get() = DatabaseHelper.getInstance(this, AppState.dbPassword!!)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etDate.setText(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )

        binding.btnBack.setOnClickListener { finish() }

        binding.btnSave.setOnClickListener {
            val date  = binding.etDate.text.toString().trim()
            val title = binding.etTitle.text.toString().trim()
            val text  = binding.etText.text.toString().trim()

            var valid = true
            if (date.isBlank()) {
                binding.tilDate.error = getString(R.string.field_required)
                valid = false
            } else {
                binding.tilDate.error = null
            }
            if (title.isBlank()) {
                binding.tilTitle.error = getString(R.string.field_required)
                valid = false
            } else {
                binding.tilTitle.error = null
            }
            if (!valid) return@setOnClickListener

            db.addEntry(Entry(date = date, title = title, text = text))
            Toast.makeText(this, getString(R.string.entry_saved), Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
