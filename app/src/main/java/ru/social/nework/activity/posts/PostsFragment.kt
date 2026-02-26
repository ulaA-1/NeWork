package ru.social.nework.activity.posts

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.social.nework.R
import ru.social.nework.activity.posts.PostDetailsFragment.Companion.longArg
import ru.social.nework.adapter.OnInteractionListener
import ru.social.nework.adapter.PostAdapter
import ru.social.nework.auth.AppAuth
import ru.social.nework.databinding.FragmentPostsBinding
import ru.social.nework.dto.Post
import ru.social.nework.util.AndroidUtils.showSignInDialog
import ru.social.nework.util.MediaLifecycleObserver
import ru.social.nework.viewmodel.PostViewModel
import javax.inject.Inject

@AndroidEntryPoint
class PostsFragment : Fragment() {
    @Inject
    lateinit var auth: AppAuth

    private val postViewModel: PostViewModel by viewModels(ownerProducer = ::requireActivity)
    private val mediaObserver = MediaLifecycleObserver()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPostsBinding.inflate(inflater, container, false)
        lifecycle.addObserver(mediaObserver)
        val adapter = PostAdapter(object : OnInteractionListener {
            override fun onEdit(post: Post) {
                findNavController().navigate(R.id.action_postsFragment_to_newPostFragment,
                    Bundle().apply{
                        longArg = post.id
                    })
            }

            override fun onLike(post: Post){
                if(auth.authenticated()){
                    postViewModel.likeById(post)
                } else showSignInDialog(this@PostsFragment)
            }

            override fun onRemove(post: Post) {
                postViewModel.removeById(post)
            }

            override fun onShare(post: Post) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, post.content)
                    type = "text/plain"
                }

                val shareIntent =
                    Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(shareIntent)
            }

            override fun onItemClick(post: Post) {
                findNavController().navigate(R.id.action_postsFragment_to_postDetailsFragment,
                    Bundle().apply{
                    longArg = post.id
                })
            }

            override fun onPlayAudio(post: Post, seekBar: SeekBar, playAudio: ImageButton) {
                mediaObserver.playAudio(post.attachment!!, seekBar, playAudio)
            }

        }, requireContext(), auth.authenticated(), mediaObserver)

        binding.list.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                postViewModel.data.collectLatest(adapter::submitData)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                adapter.loadStateFlow.collectLatest { state ->
                    binding.swiperefresh.isRefreshing =
                        state.refresh is LoadState.Loading ||
                                state.prepend is LoadState.Loading ||
                                state.append is LoadState.Loading
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            postViewModel.newerCount.collectLatest{
                binding.newPosts.isVisible = it > 0
                println(it)
            }
        }

        postViewModel.dataState.observe(viewLifecycleOwner){state ->
            binding.progress.isVisible = state.loading
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .show()
                postViewModel.resetError()
            }
        }

        binding.newPosts.setOnClickListener {
            postViewModel.readNewPosts()
            binding.newPosts.isVisible = false
            binding.list.smoothScrollToPosition(0)
        }


        binding.fab.setOnClickListener {
            if(auth.authenticated()){
                postViewModel.edit(null)
                findNavController().navigate(R.id.action_postsFragment_to_newPostFragment)
            } else showSignInDialog(this)

        }

        binding.swiperefresh.setOnRefreshListener(adapter::refresh)
        return binding.root
    }



}