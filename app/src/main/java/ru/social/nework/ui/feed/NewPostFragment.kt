package ru.social.nework.ui.feed

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import ru.social.nework.R
import ru.social.nework.databinding.FragmentNewPostBinding

@AndroidEntryPoint
class NewPostFragment : Fragment(R.layout.fragment_new_post) {

    private val viewModel: NewPostViewModel by viewModels()

    private var _binding: FragmentNewPostBinding? = null
    private val binding get() = _binding!!

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            viewModel.setAttachment(AttachmentDraft.Image(uri))
        }

    private val pickAudio =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            viewModel.setAttachment(AttachmentDraft.Audio(uri))
        }

    private val pickVideo =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            viewModel.setAttachment(AttachmentDraft.Video(uri))
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentNewPostBinding.bind(view)
        setHasOptionsMenu(true)

        binding.removeAttachment.setOnClickListener {
            viewModel.clearAttachment()
        }

        binding.btnPhoto.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnAttachment.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.pick_attachment_title))
                .setItems(
                    arrayOf(
                        getString(R.string.pick_attachment_audio),
                        getString(R.string.pick_attachment_video),
                    )
                ) { _, which ->
                    when (which) {
                        0 -> pickAudio.launch("audio/*")
                        1 -> pickVideo.launch("video/*")
                    }
                }
                .show()
        }

        binding.btnMention.setOnClickListener {
            Toast.makeText(requireContext(), R.string.todo_pick_mentions, Toast.LENGTH_SHORT).show()
        }

        binding.btnLocation.setOnClickListener {
            Toast.makeText(requireContext(), R.string.todo_pick_location, Toast.LENGTH_SHORT).show()
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.previewCard.isVisible = state.attachment != null

            when (val a = state.attachment) {
                is AttachmentDraft.Image -> {
                    binding.preview.setImageURI(a.uri)
                }

                is AttachmentDraft.Video -> {
                    binding.preview.setImageResource(R.drawable.ic_play_24)
                }

                is AttachmentDraft.Audio -> {
                    binding.preview.setImageResource(R.drawable.ic_music_note_24)
                }

                null -> Unit
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { ev ->
            ev ?: return@observe

            when (ev) {
                is NewPostViewModel.Event.Success -> {
                    Toast.makeText(requireContext(), R.string.post_created, Toast.LENGTH_SHORT)
                        .show()
                }

                is NewPostViewModel.Event.Error -> {
                    when (ev.message) {
                        "empty" -> Toast.makeText(
                            requireContext(),
                            R.string.error_post_empty,
                            Toast.LENGTH_SHORT
                        ).show()

                        "too_big" -> Toast.makeText(
                            requireContext(),
                            R.string.error_attachment_too_big,
                            Toast.LENGTH_LONG
                        ).show()

                        else -> Toast.makeText(requireContext(), ev.message, Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }

            viewModel.consumeEvent()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_new_post, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                val text = binding.content.text?.toString().orEmpty()
                viewModel.save(text) { findNavController().navigateUp() }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}