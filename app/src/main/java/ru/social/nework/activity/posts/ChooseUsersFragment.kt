package ru.social.nework.activity.posts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import ru.social.nework.adapter.ChooseUserAdapter
import ru.social.nework.adapter.OnInteractionListener
import ru.social.nework.databinding.FragmentChooseMentionBinding
import ru.social.nework.dto.User
import ru.social.nework.util.LongArrayArg
import ru.social.nework.viewmodel.PostViewModel
import ru.social.nework.viewmodel.UserViewModel

@AndroidEntryPoint
class ChooseUsersFragment : Fragment() {

    companion object {
        var Bundle.longArrayArg: LongArray? by LongArrayArg
    }

    private val userViewModel: UserViewModel by viewModels()
    private val postViewModel: PostViewModel by viewModels(ownerProducer = ::requireActivity)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentChooseMentionBinding.inflate(inflater,
            container,
            false
        )
        userViewModel.loadUsers()
        val checkedUsers = arguments?.longArrayArg ?: arrayOf<Long>().toLongArray()
        val adapter = ChooseUserAdapter(
            requireContext(),
            checkedUsers,
            object : OnInteractionListener {
            override fun onCheck(user: User, checked: Boolean) {
                if(checked){
                    postViewModel.chooseUser(user)
                } else
                    postViewModel.removeUser(user)
            }
        })
        binding.list.adapter = adapter


        userViewModel.data.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)
        }
        return binding.root
    }

}