package dadm.grupo.dadmproyecto.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
// Remove layout ID from constructor if inflating manually
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

        val languageAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.language_options,
            android.R.layout.simple_spinner_item
        )
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLanguage.adapter = languageAdapter

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
            val selectedLanguage =
                if (binding.spinnerLanguage.selectedItemPosition == 0) "es" else "en"

            viewModel.updateUser(username, bio, selectedLanguage)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
