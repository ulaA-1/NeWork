package ru.social.nework.activity.users

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.social.nework.R
import ru.social.nework.adapter.OnInteractionListener
import ru.social.nework.adapter.PostAdapter
import ru.social.nework.auth.AppAuth
import ru.social.nework.databinding.FragmentUserPostsBinding
import ru.social.nework.dto.Post
import ru.social.nework.util.AndroidUtils
import ru.social.nework.util.MediaLifecycleObserver
import ru.social.nework.viewmodel.UserViewModel
import ru.social.nework.viewmodel.WallViewModel
import javax.inject.Inject
@AndroidEntryPoint
class UserPostsFragment : Fragment() {
    @Inject
    lateinit var auth: AppAuth
    private val userViewModel: UserViewModel by viewModels(ownerProducer = ::requireActivity)
    @Inject
    lateinit var factory: WallViewModel.Factory

    private val wallViewModel: WallViewModel by viewModels {
        WallViewModel.provideWallViewModelFactory(
            factory,
            userViewModel.selectedUser.value!!
            )
    }
    private lateinit var binding: FragmentUserPostsBinding
    private val mediaObserver = MediaLifecycleObserver()
    private var postPlaying: Post? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentUserPostsBinding.inflate(layoutInflater, container, false)

        val adapter = PostAdapter(object : OnInteractionListener {
            override fun onEdit(post: Post) {
                val request = NavDeepLinkRequest.Builder
                    .fromUri("android-app://newPostFragment?longArg=${post.id}".toUri())
                    .build()
                findNavController().navigate(request)
            }

            override fun onLike(post: Post) {
                if(auth.authenticated()){
                    wallViewModel.likeById(post)
                } else AndroidUtils.showSignInDialog(this@UserPostsFragment)
            }

            override fun onRemove(post: Post) {
                wallViewModel.removeById(post)
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
                val request = NavDeepLinkRequest.Builder
                    .fromUri("android-app://postDetailsFragment?longArg=${post.id}".toUri())
                    .build()
                findNavController().navigate(request)
            }

            override fun onPlayAudio(post: Post, seekBar: SeekBar, playAudio: ImageButton) {
                mediaObserver.playAudio(post.attachment!!, seekBar, playAudio)
                postPlaying = post
            }


        }, requireContext(), auth.authenticated(), mediaObserver)

        binding.list.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                wallViewModel.data.collectLatest(adapter::submitData)
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

        wallViewModel.dataState.observe(viewLifecycleOwner){state ->
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .show()
                wallViewModel.resetError()
            }
        }

        binding.swiperefresh.setOnRefreshListener(adapter::refresh)
        return binding.root
    }


}