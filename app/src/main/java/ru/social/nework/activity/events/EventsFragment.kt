package ru.social.nework.activity.events

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
import ru.social.nework.adapter.EventAdapter
import ru.social.nework.adapter.EventOnInteractionListener
import ru.social.nework.auth.AppAuth
import ru.social.nework.databinding.FragmentEventsBinding
import ru.social.nework.dto.Event
import ru.social.nework.util.AndroidUtils.showSignInDialog
import ru.social.nework.util.MediaLifecycleObserver
import ru.social.nework.viewmodel.EventViewModel
import javax.inject.Inject

@AndroidEntryPoint
class EventsFragment : Fragment() {
    @Inject
    lateinit var auth: AppAuth

    private val eventViewModel: EventViewModel by viewModels(ownerProducer = ::requireActivity)
    private val mediaObserver = MediaLifecycleObserver()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentEventsBinding.inflate(inflater, container, false)
        lifecycle.addObserver(mediaObserver)
        val adapter = EventAdapter.EventAdapter(object : EventOnInteractionListener {
            override fun onEdit(event: Event) {
                eventViewModel.edit(event)
                findNavController().navigate(R.id.action_eventsFragment_to_newEventFragment)
            }

            override fun onLike(event: Event) {
                if(auth.authenticated()){
                    eventViewModel.likeById(event)
                } else showSignInDialog(this@EventsFragment)
            }

            override fun onParticipate(event: Event) {
                if(auth.authenticated()){
                    eventViewModel.participateById(event)
                } else showSignInDialog(this@EventsFragment)
            }

            override fun onRemove(event: Event) {
                eventViewModel.removeById(event)
            }

            override fun onShare(event: Event) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, event.content)
                    type = "text/plain"
                }

                val shareIntent =
                    Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(shareIntent)
            }

            override fun onItemClick(event: Event) {
                findNavController().navigate(
                    R.id.action_eventsFragment_to_eventDetailsFragment,
                    Bundle().apply {
                        longArg = event.id
                    })
            }

            override fun onPlayAudio(event: Event, seekBar: SeekBar, playAudio: ImageButton) {
                mediaObserver.playAudio(event.attachment!!, seekBar, playAudio)
            }


        }, requireContext(), auth.authenticated(), mediaObserver)

        binding.list.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                eventViewModel.data.collectLatest(adapter::submitData)
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
            eventViewModel.newerCount.collectLatest{
                binding.newEvents.isVisible = it > 0
                println(it)
            }
        }

        binding.newEvents.setOnClickListener {
            eventViewModel.readNewEvents()
            binding.newEvents.isVisible = false
            binding.list.smoothScrollToPosition(0)
        }

        eventViewModel.dataState.observe(viewLifecycleOwner){state ->
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .show()
                eventViewModel.resetError()
            }
        }

        binding.fab.setOnClickListener {
            if(auth.authenticated()){
                eventViewModel.edit(null)
                findNavController().navigate(R.id.action_eventsFragment_to_newEventFragment)
            } else showSignInDialog(this)

        }

        binding.swiperefresh.setOnRefreshListener(adapter::refresh)
        return binding.root
    }

    }
