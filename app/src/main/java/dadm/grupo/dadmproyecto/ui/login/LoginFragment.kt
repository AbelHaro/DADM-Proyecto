package dadm.grupo.dadmproyecto.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.databinding.FragmentLoginBinding
import dadm.grupo.dadmproyecto.ui.MainActivity

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGoToRegister.setOnClickListener {
            navigateToRegister()
        }


        binding.btnLogin.setOnClickListener {
            // Aquí iría tu lógica de autenticación
        }

        binding.tvGoToMainActivity.setOnClickListener {
            binding.tvGoToMainActivity.setOnClickListener {
                val intent = Intent(requireActivity(), MainActivity::class.java)
                startActivity(intent)
            }
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