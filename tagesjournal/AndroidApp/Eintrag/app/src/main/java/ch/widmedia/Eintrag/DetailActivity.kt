package ch.widmedia.Eintrag

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ch.widmedia.Eintrag.databinding.ActivityDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private val db get() = DatabaseHelper.getInstance(this, AppState.dbPassword!!)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val entryId = intent.getLongExtra(ListActivity.EXTRA_ENTRY_ID, -1L)
        if (entryId == -1L) { finish(); return }

        val entry = db.getEntry(entryId)
        if (entry == null) { finish(); return }

        binding.tvDate.text  = entry.date
        binding.tvTitle.text = entry.title
        binding.tvText.text  = entry.text

        binding.btnBack.setOnClickListener { finish() }

        binding.btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.delete_confirm_title))
                .setMessage(getString(R.string.delete_confirm_msg, entry.title))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    db.deleteEntry(entry.id)
                    Toast.makeText(this, getString(R.string.entry_deleted), Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }
}
