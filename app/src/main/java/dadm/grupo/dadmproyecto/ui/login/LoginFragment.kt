package dadm.grupo.dadmproyecto.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.databinding.FragmentLoginBinding
import dadm.grupo.dadmproyecto.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

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
        setupClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupClickListeners() {
        binding.btnGoToRegister.setOnClickListener { navigateToRegister() }
        binding.btnLogin.setOnClickListener { handleLogin() }
        binding.btnGoToMainActivityUnlogged.setOnClickListener { navigateToMainActivity() }
        binding.btnGoToMainActivityLogged.setOnClickListener { navigateToMainActivityLogged() }
        binding.btnGoogleSignIn.setOnClickListener { handleGoogleSignIn() }
    }

    private fun handleLogin() {
        val email = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString()

        when {
            email.isEmpty() || password.isEmpty() -> {
                showToast(getString(R.string.login_error_empty_fields))
                return
            }

            !isValidEmail(email) -> {
                showToast(getString(R.string.login_error_invalid_email))
                return
            }

            else -> {
                viewModel.loginUser(email, password) { success, errorMessage ->
                    if (success) {
                        navigateToMainActivity()
                    } else {
                        showToast(
                            getString(
                                R.string.login_error_authentication_failed,

                                ) + errorMessage.toString()
                        )
                    }
                }
            }
        }
    }

    private fun navigateToRegister() {
        findNavController().navigate(R.id.actionLoginFragmentToRegisterFragment)
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(requireActivity(), MainActivity::class.java))
    }

    private fun navigateToMainActivityLogged() {
        val email = "test@gmail.com"
        val password = "test123"

        viewModel.loginUser(email, password) { success, errorMessage ->
            if (success) {
                navigateToMainActivity()
            } else {
                showToast(
                    getString(
                        R.string.login_error_authentication_failed,
                    ) + errorMessage.toString()
                )
            }
        }
    }

    private fun handleGoogleSignIn() {
        showToast(getString(R.string.login_google_sign_in_not_implemented))
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(requireContext(), message, duration).show()
    }
}
