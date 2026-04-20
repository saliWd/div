package com.securenotes.app.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.securenotes.app.R
import com.securenotes.app.databinding.FragmentNoteEditorBinding
import com.securenotes.app.viewmodel.NoteEditorViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NoteEditorFragment : Fragment() {

    private var _binding: FragmentNoteEditorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NoteEditorViewModel by viewModels()
    private val args: NoteEditorFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNoteEditorBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadNote(args.noteId)

        viewModel.note.observe(viewLifecycleOwner) { note ->
            note?.let {
                binding.etTitle.setText(it.title)
                binding.etContent.setText(it.content)
            }
        }

        viewModel.saved.observe(viewLifecycleOwner) { saved ->
            if (saved) findNavController().navigateUp()
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_editor, menu)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                val title   = binding.etTitle.text.toString().trim()
                val content = binding.etContent.text.toString().trim()
                viewModel.saveNote(title, content)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
