package ru.social.nework.ui.auth

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.social.nework.R
import ru.social.nework.databinding.FragmentRegisterBinding
import androidx.appcompat.content.res.AppCompatResources

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    private var passVisible = false
    private var repeatVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPasswordToggles()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupPasswordToggles() {
        binding.regPassLayout.setEndIconOnClickListener {
            passVisible = !passVisible
            binding.regPassEdit.transformationMethod =
                if (passVisible) null else PasswordTransformationMethod.getInstance()
            binding.regPassEdit.setSelection(binding.regPassEdit.text?.length ?: 0)
            binding.regPassLayout.endIconDrawable = AppCompatResources.getDrawable(
                requireContext(),
                if (passVisible) R.drawable.ic_visibility_24 else R.drawable.ic_visibility_off_24
            )
        }

        binding.regRepeatLayout.setEndIconOnClickListener {
            repeatVisible = !repeatVisible
            binding.regRepeatEdit.transformationMethod =
                if (repeatVisible) null else PasswordTransformationMethod.getInstance()
            binding.regRepeatEdit.setSelection(binding.regRepeatEdit.text?.length ?: 0)
            binding.regRepeatLayout.endIconDrawable = AppCompatResources.getDrawable(
                requireContext(),
                if (repeatVisible) R.drawable.ic_visibility_24 else R.drawable.ic_visibility_off_24
            )
        }
    }

    private fun setupClickListeners() {
        binding.registerButton.setOnClickListener {
            val login = binding.regLoginEdit.text.toString()
            val name = binding.regNameEdit.text.toString()
            val pass = binding.regPassEdit.text.toString()
            val repeatPass = binding.regRepeatEdit.text.toString()

            viewModel.register(login, pass, repeatPass, name)
        }

        binding.toLogin.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.registerState.observe(viewLifecycleOwner) { state ->
                    when (state) {
                        is AuthUiState.Loading -> {
                            binding.registerButton.isEnabled = false
                        }

                        is AuthUiState.Success -> {
                            binding.registerButton.isEnabled = true
                            Toast.makeText(
                                requireContext(),
                                R.string.register_success,
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().popBackStack()
                        }

                        is AuthUiState.Error -> {
                            binding.registerButton.isEnabled = true
                            Toast.makeText(requireContext(), state.messageRes, Toast.LENGTH_LONG)
                                .show()
                        }

                        else -> {
                            binding.registerButton.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}