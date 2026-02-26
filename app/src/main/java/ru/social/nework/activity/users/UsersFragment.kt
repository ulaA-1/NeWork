package ru.social.nework.activity.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.social.nework.R
import ru.social.nework.activity.posts.PostDetailsFragment.Companion.longArg
import ru.social.nework.adapter.OnInteractionListener
import ru.social.nework.adapter.UserAdapter
import ru.social.nework.databinding.FragmentUsersBinding
import ru.social.nework.dto.User
import ru.social.nework.viewmodel.UserViewModel
@AndroidEntryPoint
class UsersFragment : Fragment() {

    private val userViewModel: UserViewModel by viewModels(ownerProducer = ::requireActivity)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding = FragmentUsersBinding.inflate(inflater, container, false)
        val adapter = UserAdapter(object : OnInteractionListener {
            override fun onUserClick(user: User) {
                findNavController().navigate(
                    R.id.action_usersFragment_to_userWallFragment,
                    Bundle().apply{
                        longArg = user.id
                    })
            }

        }, requireContext())

        binding.list.adapter = adapter

        userViewModel.data.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)
        }

        userViewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            binding.swiperefresh.isRefreshing = state.refreshing
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry_loading) { userViewModel.loadUsers() }
                    .show()
            }
        }

        binding.swiperefresh.setOnRefreshListener {
            userViewModel.loadUsers()
            binding.swiperefresh.isRefreshing = false
        }
        return binding.root
    }

}