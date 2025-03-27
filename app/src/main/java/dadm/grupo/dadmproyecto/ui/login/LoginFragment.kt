package dadm.grupo.dadmproyecto.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.initialize
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.databinding.FragmentLoginBinding
import dadm.grupo.dadmproyecto.ui.MainActivity

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.btnGoToRegister.setOnClickListener {
            navigateToRegister()
        }


        binding.btnLogin.setOnClickListener {
            val email = binding.etUsername.text.toString()
            val password = binding.etPassword.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Inicio de sesión exitoso
                            val intent = Intent(requireActivity(), MainActivity::class.java)
                            startActivity(intent)
                        } else {
                            // Manejar el error de inicio de sesión
                            Toast(requireContext()).apply {
                                setText("Error al iniciar sesión: ${task.exception?.message}")
                                show()
                            }
                        }
                    }
            } else {
                // Manejar el caso en que los campos están vacíos
                Toast(requireContext()).apply {
                    setText("Por favor, completa todos los campos")
                    show()
                }
            }

        }

        binding.btnGoToMainActivity.setOnClickListener {
            binding.btnGoToMainActivity.setOnClickListener {
                val intent = Intent(requireActivity(), MainActivity::class.java)
                startActivity(intent)
            }
        }

        binding.btnGoogleSignIn.setOnClickListener {
            // Aquí iría tu lógica de inicio de sesión con Google
            Firebase.initialize(requireContext())

        }
    }

    private fun navigateToRegister() {
        // Navegar al RegisterFragment usando Navigation Component
        findNavController().navigate(R.id.actionLoginFragmentToRegisterFragment)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
