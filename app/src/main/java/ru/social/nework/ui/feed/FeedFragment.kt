package ru.social.nework.ui.feed

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import ru.social.nework.R
import ru.social.nework.ui.auth.AppAuth
import javax.inject.Inject

@AndroidEntryPoint
class FeedFragment : Fragment(R.layout.fragment_feed) {

    private val viewModel: FeedViewModel by viewModels()

    @Inject
    lateinit var appAuth: AppAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val list = view.findViewById<RecyclerView>(R.id.list)
        val fab = view.findViewById<FloatingActionButton>(R.id.fab)

        val adapter = PostsAdapter(
            onMenu = { anchor, post -> showMenu(anchor, post) },
            onLike = { post -> viewModel.likePost(post) },
            onShare = { post -> sharePost(post) },
            onClick = { post -> openPostDetail(post) }
        )
        list.adapter = adapter

        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }

        fab.setOnClickListener {
            val isAuthenticated = appAuth.myId != 0L
            if (isAuthenticated) {
                findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
            } else {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.auth_required_title)
                    .setMessage(R.string.auth_required_message_post)
                    .setPositiveButton(R.string.action_login) { _, _ ->
                        findNavController().navigate(R.id.loginFragment)
                    }
                    .setNegativeButton(R.string.action_register) { _, _ ->
                        findNavController().navigate(R.id.registerFragment)
                    }
                    .setNeutralButton(R.string.action_cancel, null)
                    .show()
            }
        }
    }

    private fun showMenu(anchor: View, post: PostUi) {
        if (!post.ownedByMe) return

        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.post_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    editPost(post)
                    true
                }

                R.id.action_delete -> {
                    confirmDelete(post)
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    private fun confirmDelete(post: PostUi) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_confirmation_title)
            .setMessage(R.string.delete_post_message)
            .setPositiveButton(R.string.action_yes) { _, _ ->
                viewModel.deletePost(post)
            }
            .setNegativeButton(R.string.action_no, null)
            .show()
    }

    private fun sharePost(post: PostUi) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, post.content)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_chooser_title)))
    }

    private fun editPost(post: PostUi) {
        Toast.makeText(
            requireContext(),
            getString(R.string.toast_edit_post, post.id),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun openPostDetail(post: PostUi) {
        Toast.makeText(
            requireContext(),
            getString(R.string.toast_open_post, post.id),
            Toast.LENGTH_SHORT
        ).show()
    }
}