package ru.social.nework.activity.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import ru.social.nework.R
import ru.social.nework.adapter.FragmentPageAdapter
import ru.social.nework.auth.AppAuth
import ru.social.nework.databinding.FragmentUserWallBinding
import ru.social.nework.util.LongArg
import ru.social.nework.viewmodel.UserViewModel
import javax.inject.Inject

@AndroidEntryPoint
class UserWallFragment : Fragment() {
    @Inject
    lateinit var auth: AppAuth
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager2: ViewPager2
    private lateinit var adapter: FragmentPageAdapter
    private val userViewModel: UserViewModel by viewModels(ownerProducer = ::requireActivity)
    private lateinit var binding: FragmentUserWallBinding

    companion object {
        var Bundle.longArg: Long? by LongArg
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserWallBinding.inflate(layoutInflater, container, false)

        val collapsingToolbarLayout = binding.collapsingToolbar
        val toolbar = binding.toolbar

        val navController = findNavController()

        val appBarConfiguration= AppBarConfiguration(navController.graph)

        setupWithNavController(
            toolbar, navController, appBarConfiguration
        )

        setupWithNavController(collapsingToolbarLayout, toolbar, navController)
        val userId = arguments?.longArg ?: -1
        userViewModel.selectUser(userId)
        userViewModel.data.observe(viewLifecycleOwner) { users ->
            val user = users.find { it.id == userId }
            if(user != null){
                binding.apply {
                    Glide.with(avatar)
                        .load(user.avatar)
                        .placeholder(R.drawable.ic_loading_100dp)
                        .error(R.drawable.ic_error_100dp)
                        .centerCrop()
                        .timeout(10_000)
                        .into(binding.avatar)
                }
                collapsingToolbarLayout.isTitleEnabled = true
                collapsingToolbarLayout.title = "${user.name}/${user.login}"
        }

        }
        tabLayout = binding.tabs
        viewPager2 = binding.viewPager
        adapter = FragmentPageAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        tabLayout.addTab(tabLayout.newTab().setText("Wall"))
        tabLayout.addTab(tabLayout.newTab().setText("Jobs"))

        viewPager2.adapter = adapter
        TabLayoutMediator(tabLayout, viewPager2){tab, index ->
            when(index){
                0 -> {
                    tab.text = "Wall"
                    binding.fab.visibility = View.GONE
                }
                1 -> {
                    tab.text = "Jobs"
                    binding.fab.visibility = View.VISIBLE
                }
                else -> Unit
            }

        }.attach()
        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when(position){
                    0 -> {
                        binding.fab.visibility = View.GONE
                    }
                    1 -> {
                        binding.fab.visibility = if(userId == auth.authStateFlow.value.id) View.VISIBLE else View.GONE
                    }
                    else -> Unit
                }
            }
        })
        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_userWallFragment_to_newJobFragment)
        }
        return binding.root
    }

}