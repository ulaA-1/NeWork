package ru.social.nework.ui.auth

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.social.nework.R
import ru.social.nework.databinding.FragmentLoginBinding

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPasswordToggle()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupPasswordToggle() {
        var visible = false

        binding.passwordLayout.setEndIconOnClickListener {
            visible = !visible

            binding.passwordEdit.transformationMethod =
                if (visible) null else PasswordTransformationMethod.getInstance()

            binding.passwordEdit.setSelection(binding.passwordEdit.text?.length ?: 0)

            binding.passwordLayout.endIconDrawable = AppCompatResources.getDrawable(
                requireContext(),
                if (visible) R.drawable.ic_visibility_24 else R.drawable.ic_visibility_off_24
            )
        }
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            val login = binding.loginEdit.text.toString()
            val pass = binding.passwordEdit.text.toString()
            viewModel.login(login, pass)
        }

        binding.toRegister.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthUiState.Loading -> binding.loginButton.isEnabled = false

                is AuthUiState.Success -> {
                    binding.loginButton.isEnabled = true
                    Toast.makeText(requireContext(), R.string.login_success, Toast.LENGTH_SHORT)
                        .show()
                    findNavController().popBackStack()
                }

                is AuthUiState.Error -> {
                    binding.loginButton.isEnabled = true
                    Toast.makeText(requireContext(), getString(state.messageRes), Toast.LENGTH_LONG)
                        .show()
                }

                else -> binding.loginButton.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}