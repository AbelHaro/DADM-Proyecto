package dadm.grupo.dadmproyecto.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.domain.model.User
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

        tvDisplayName = view.findViewById(R.id.tvDisplayName)
        tvBio = view.findViewById(R.id.tvBio)
        tvLocationsDiscovered = view.findViewById(R.id.tvLocationsDiscovered)
        progressBar = view.findViewById(R.id.progressDiscovered)

        view.findViewById<View>(R.id.btnEditProfile).setOnClickListener {
            // TODO: Navegar a editar perfil
        }

        view.findViewById<View>(R.id.btnSettings).setOnClickListener {
            // TODO: Navegar a ajustes
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadUserProfile()

        lifecycleScope.launch {
            viewModel.userState.collectLatest { user ->
                user?.let { showUserProfile(it) }
            }
        }
    }

    private fun showUserProfile(user: User) {
        tvDisplayName.text = user.displayName
        tvBio.text = user.bio
        val visited = 0
        tvLocationsDiscovered.text = "Has descubierto $visited de $totalLocations lugares secretos"
        progressBar.max = totalLocations
        progressBar.progress = visited
    }
}
