package ru.social.nework.activity.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import ru.social.nework.adapter.OnInteractionListener
import ru.social.nework.adapter.UserAdapter
import ru.social.nework.databinding.FragmentSpeakersBinding
import ru.social.nework.viewmodel.EventViewModel
@AndroidEntryPoint
class SpeakersFragment : Fragment() {

    private val eventViewModel: EventViewModel by viewModels(ownerProducer = ::requireActivity)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSpeakersBinding.inflate(inflater, container, false)
        val adapter = UserAdapter(object : OnInteractionListener {
        }, requireContext())

        binding.list.adapter = adapter

        eventViewModel.speakers.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)
        }
        return binding.root
    }

}