package ru.social.nework.activity.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.social.nework.R
import ru.social.nework.adapter.JobAdapter
import ru.social.nework.adapter.OnInteractionListener
import ru.social.nework.databinding.FragmentUserJobsBinding
import ru.social.nework.dto.Job
import ru.social.nework.viewmodel.JobViewModel
import ru.social.nework.viewmodel.UserViewModel
import javax.inject.Inject

@AndroidEntryPoint
class UserJobsFragment : Fragment() {

    private val userViewModel: UserViewModel by viewModels(ownerProducer = ::requireActivity)

    @Inject
    lateinit var factory: JobViewModel.Factory

    private val jobViewModel: JobViewModel by viewModels {
        JobViewModel.provideJobViewModelFactory(
            factory,
            userViewModel.selectedUser.value!!
        )
    }
    private lateinit var binding: FragmentUserJobsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserJobsBinding.inflate(layoutInflater, container, false)
        userViewModel.selectedUser.observe(viewLifecycleOwner) { _ ->
            jobViewModel.loadJobs()
        }
        val adapter = JobAdapter(object : OnInteractionListener {

            override fun onJobDelete(job: Job) {
                jobViewModel.removeById(job)
            }

        })

        binding.list.adapter = adapter

        jobViewModel.data.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.data)
            binding.emptyText.isVisible = state.empty
        }

        jobViewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            binding.swiperefresh.isRefreshing = state.refreshing
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry_loading) {
                        jobViewModel.loadJobs()
                    }
                    .show()
                jobViewModel.resetError()
            }
        }


        binding.swiperefresh.setOnRefreshListener {
            jobViewModel.loadJobs()
            binding.swiperefresh.isRefreshing = false
        }
        return binding.root
    }


}