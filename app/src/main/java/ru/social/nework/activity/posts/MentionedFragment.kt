package ru.social.nework.activity.posts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import ru.social.nework.adapter.OnInteractionListener
import ru.social.nework.adapter.UserAdapter
import ru.social.nework.databinding.FragmentMentionedBinding
import ru.social.nework.viewmodel.PostViewModel
@AndroidEntryPoint
class MentionedFragment : Fragment() {
    private val postViewModel: PostViewModel by viewModels(ownerProducer = ::requireActivity) /*{
        PostViewModel.PostViewModelFactory(
            requireActivity().application
        )
    }*/

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentMentionedBinding.inflate(inflater, container, false)
        val adapter = UserAdapter(object : OnInteractionListener {
            /*override fun onUserClick(user: User) {
                findNavController().navigate(
                    R.id.action_usersFragment_to_userWallFragment)
            }*/

        }, requireContext())

        binding.list.adapter = adapter

        postViewModel.mentioned.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)
        }
        return binding.root
    }
}