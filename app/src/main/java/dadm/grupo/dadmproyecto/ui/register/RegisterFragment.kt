package dadm.grupo.dadmproyecto.ui.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment(R.layout.fragment_register) {

    // ViewBinding
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    // Firebase Authentication
    private lateinit var firebaseAuth: FirebaseAuth

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
        initializeFirebaseAuth()
        setupClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Initialize Firebase Authentication instance
     */
    private fun initializeFirebaseAuth() {
        firebaseAuth = FirebaseAuth.getInstance()
    }

    /**
     * Set up all click listeners for the fragment
     */
    private fun setupClickListeners() {
        binding.btnBackToLogin.setOnClickListener { navigateToLogin() }
        binding.btnRegister.setOnClickListener { handleRegistration() }
    }

    /**
     * Handle the registration process when the register button is clicked
     */
    private fun handleRegistration() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        when {
            email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                showToast(getString(R.string.register_error_empty_fields), Toast.LENGTH_LONG)
                return
            }

            !isValidEmail(email) -> {
                showToast(getString(R.string.register_error_invalid_email), Toast.LENGTH_LONG)
                return
            }

            !isValidPassword(password) -> {
                showToast(getString(R.string.register_error_weak_password), Toast.LENGTH_LONG)
                return
            }

            password != confirmPassword -> {
                showToast(getString(R.string.register_error_password_mismatch), Toast.LENGTH_LONG)
                return
            }

            else -> registerUser(email, password)
        }
    }

    /**
     * Attempt to register a new user with Firebase Authentication
     * @param email User's email address
     * @param password User's password
     */
    private fun registerUser(email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    navigateToLogin()
                } else {
                    showToast(
                        getString(
                            R.string.register_error_registration_failed,

                            ) + task.exception?.message,
                        Toast.LENGTH_LONG
                    )
                }
            }
    }

    /**
     * Navigate back to the login screen
     */
    private fun navigateToLogin() {
        findNavController().navigate(R.id.actionRegisterFragmentToLoginFragment)
    }

    /**
     * Validate email format using Android Patterns
     * @param email Email address to validate
     * @return true if email is valid, false otherwise
     */
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Validate password meets security requirements
     * @param password Password to validate
     * @return true if password is valid (min 8 chars, contains letter and number)
     */
    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isDigit() } &&
                password.any { it.isLetter() }
    }

    /**
     * Helper function to show Toast messages
     * @param message The message to display
     */
    private fun showToast(message: String, lengthLong: Int) {
        Toast.makeText(requireContext(), message, lengthLong).show()
    }
}
