package dadm.grupo.dadmproyecto.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dadm.grupo.dadmproyecto.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Language spinner setup removed - using system language

        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.etUsername.setText(it.displayName)
                binding.etBio.setText(it.bio)
            }
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    Toast.makeText(
                        requireContext(),
                        "Usuario actualizado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    // No language change handling needed - just return
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Error al actualizar el usuario: ${it.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.btnSaveChanges.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val bio = binding.etBio.text.toString()

            // No language parameter needed
            viewModel.updateUser(username, bio)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
