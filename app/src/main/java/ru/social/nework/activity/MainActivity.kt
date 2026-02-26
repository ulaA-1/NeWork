package ru.social.nework.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.social.nework.R
import ru.social.nework.auth.AppAuth
import ru.social.nework.databinding.ActivityMainBinding
import ru.social.nework.util.SignOutDialogFragment
import ru.social.nework.viewmodel.AuthViewModel
import ru.social.nework.viewmodel.EventViewModel
import ru.social.nework.viewmodel.PostViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var auth: AppAuth
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var showMenu: Boolean = true
    private val postViewModel: PostViewModel by viewModels()
    private val eventViewModel: EventViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent?.let {
            if (it.action != Intent.ACTION_SEND) {
                return@let
            }

            val text = it.getStringExtra(Intent.EXTRA_TEXT)
            if (text?.isNotBlank() != true) {
                return@let
            }

            intent.removeExtra(Intent.EXTRA_TEXT)
        }
        viewModel.data.observe(this) {
            invalidateOptionsMenu()
        }

        val toolbar = binding.appbar
        setSupportActionBar(toolbar)

        val bottomNavigation = binding.bottomNavigation

        navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigationPosts, R.id.navigationEvents, R.id.navigationUsers
            )
        )
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.postsFragment,
                R.id.userWallFragment
                ->{
                    postViewModel.reset()
                }
                R.id.eventsFragment -> eventViewModel.reset()
            }

            when (destination.id) {
                    R.id.postsFragment,
                    R.id.usersFragment,
                    R.id.navigationPosts,
                    R.id.eventsFragment,
                    R.id.navigationUsers
                -> {
                    bottomNavigation.isVisible = true
                    //bottomNavigation.isGone = false
                }
                else -> {
                    bottomNavigation.isGone = true
                }
            }
            when(destination.id) {
                R.id.newPostFragment,
                R.id.newEventFragment,
                R.id.postDetailsFragment,
                R.id.eventDetailsFragment
                -> {
                    supportActionBar!!.show()
                    showMenu = false
                    invalidateOptionsMenu()
                }
                R.id.userWallFragment -> supportActionBar!!.hide()

                else -> {
                    supportActionBar!!.show()
                    showMenu = true
                    invalidateOptionsMenu()
                }
            }
        }
        setupActionBarWithNavController(navController, appBarConfiguration)
        bottomNavigation.setupWithNavController(navController)


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.let {
            it.setGroupVisible(R.id.unauthenticated, !viewModel.authenticated && showMenu)
            it.setGroupVisible(R.id.authenticated, viewModel.authenticated && showMenu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.signin -> {
                val navHostFragment =
                    supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController
                navController.navigate(R.id.action_global_signInFragment)
                true
            }
            R.id.signup -> {
                val navHostFragment =
                    supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController
                navController.navigate(R.id.action_global_signUpFragment)
                true
            }
            R.id.signout -> {
                SignOutDialogFragment(auth).show(supportFragmentManager, getString(R.string.sign_out))
                true
            }
            R.id.profile -> {
                val request = NavDeepLinkRequest.Builder
                    .fromUri("android-app://userWallFragment?longArg=${viewModel.data.value!!.id}".toUri())
                    .build()
                val navHostFragment =
                    supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController
                navController.navigate(request)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean{
        return navController.navigateUp() || super.onSupportNavigateUp()
    }


}