package dadm.grupo.dadmproyecto.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.domain.model.User
import dadm.grupo.dadmproyecto.ui.AuthActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private lateinit var tvDisplayName: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvLocationsDiscovered: TextView
    private lateinit var progressBar: ProgressBar

    private val totalLocations = 15

    // Usa tu factory si es necesario
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        tvDisplayName = view.findViewById(R.id.txtUsername)
        tvBio = view.findViewById(R.id.txtBio)
        tvLocationsDiscovered = view.findViewById(R.id.txtProgressLabel)
        progressBar = view.findViewById(R.id.progressVisited)

        view.findViewById<View>(R.id.btnLogout).setOnClickListener {
            // TODO: Navegar a editar perfil
        }

        view.findViewById<View>(R.id.btnSettings).setOnClickListener {
            // TODO: Navegar a ajustes
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.alpha = 0f
        view.animate().alpha(1f).setDuration(500).start()

        viewModel.loadUserProfile()

        lifecycleScope.launch {
            viewModel.userState.collectLatest { user ->
                user?.let { showUserProfile(it) }
            }
        }

        lifecycleScope.launch {
            viewModel.locationsVisitedCount.collectLatest { visited ->
                updateVisitedLocations(visited)
            }
        }

        lifecycleScope.launch {
            viewModel.logoutState.collectLatest { result ->
                result?.let {
                    if (it.isSuccess) {
                        // Navegar a la pantalla de inicio de sesión
                        val intent = Intent(requireActivity(), AuthActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    } else {
                        // Mostrar un mensaje de error
                        Toast.makeText(requireContext(), "Error al cerrar sesión", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        view.findViewById<View>(R.id.btnLogout).setOnClickListener {
            viewModel.logout()
        }

        view.findViewById<View>(R.id.btnSettings).setOnClickListener {
            val navController = findNavController()
            navController.navigate(R.id.action_profileFragment_to_settingsFragment)
        }
    }

    private fun updateVisitedLocations(visited: Int) {
        tvLocationsDiscovered.text = "Has descubierto $visited de $totalLocations lugares secretos"
        progressBar.progress = visited
    }

    private fun showUserProfile(user: User) {
        tvDisplayName.text = user.displayName
        tvBio.text = user.bio
        val visited = viewModel.locationsVisitedCount.value
        tvLocationsDiscovered.text = "Has descubierto $visited de $totalLocations lugares secretos"
        progressBar.max = totalLocations
        progressBar.progress = visited
    }
}
