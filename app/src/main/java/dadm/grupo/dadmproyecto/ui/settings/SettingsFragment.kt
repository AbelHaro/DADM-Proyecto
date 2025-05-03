package dadm.grupo.dadmproyecto.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.textfield.TextInputEditText
import dadm.grupo.dadmproyecto.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val viewModel: SettingsViewModel by viewModels()

    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etBio: TextInputEditText
    private lateinit var spinnerLanguage: Spinner
    private lateinit var btnSave: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etUsername = view.findViewById(R.id.etUsername)
        etBio = view.findViewById(R.id.etBio)
        spinnerLanguage = view.findViewById(R.id.spinnerLanguage)
        btnSave = view.findViewById(R.id.btnSaveChanges)

        val languageAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.language_options,  // see strings.xml below
            android.R.layout.simple_spinner_item
        )
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = languageAdapter

        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                etUsername.setText(it.displayName)
                etBio.setText(it.bio)
                //val langPos = if (it.language == "es") 0 else 1
                //spinnerLanguage.setSelection(langPos)
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
        }

        btnSave.setOnClickListener {
            val username = etUsername.text.toString()
            val bio = etBio.text.toString()
            val selectedLanguage = if (spinnerLanguage.selectedItemPosition == 0) "es" else "en"

            viewModel.updateUser(username, bio, selectedLanguage)
        }
    }
}
