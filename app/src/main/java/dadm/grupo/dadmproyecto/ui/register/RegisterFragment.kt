package dadm.grupo.dadmproyecto.ui.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.databinding.FragmentRegisterBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegisterViewModel by viewModels()

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
        setupClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupClickListeners() {
        binding.btnBackToLogin.setOnClickListener { navigateToLogin() }
        binding.btnRegister.setOnClickListener { handleRegistration() }
    }

    private fun handleRegistration() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        when {
            email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                showToast(getString(R.string.register_error_empty_fields))
                return
            }

            !isValidEmail(email) -> {
                showToast(getString(R.string.register_error_invalid_email))
                return
            }

            !isValidPassword(password) -> {
                showToast(getString(R.string.register_error_weak_password))
                return
            }

            password != confirmPassword -> {
                showToast(getString(R.string.register_error_password_mismatch))
                return
            }

            else -> {
                viewModel.registerUser(email, password) { success, errorMessage ->
                    if (success) {
                        navigateToLogin()
                    } else {
                        showToast(
                            getString(
                                R.string.register_error_registration_failed,

                                )
                        )
                    }
                }
            }
        }
    }

    private fun navigateToLogin() {
        findNavController().navigate(R.id.actionRegisterFragmentToLoginFragment)
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isDigit() } &&
                password.any { it.isLetter() }
    }

    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(requireContext(), message, duration).show()
    }
}
