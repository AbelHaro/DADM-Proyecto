package dadm.grupo.dadmproyecto.ui.profile

import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.databinding.FragmentProfileBinding
import dadm.grupo.dadmproyecto.domain.model.Location
import dadm.grupo.dadmproyecto.domain.model.User
import dadm.grupo.dadmproyecto.ui.AuthActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(500).start()

        viewModel.loadUserProfile()

        lifecycleScope.launch {
            viewModel.userState.collectLatest { user ->
                user?.let { showUserProfile(it) }
            }
        }

        lifecycleScope.launch {
            viewModel.locationsVisited.combine(viewModel.allLocations) { visited, all ->
                Pair(visited, all)
            }.collectLatest { (visitedLocations, allLocations) ->
                if (allLocations.isNotEmpty()) {
                    updateVisitedLocationsProgress(visitedLocations.size, allLocations.size)
                    populateLocationsGrid(allLocations, visitedLocations)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.logoutState.collectLatest { result ->
                result?.let {
                    if (it.isSuccess) {
                        val intent = Intent(requireActivity(), AuthActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Error al cerrar sesión",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }

        binding.btnSettings.setOnClickListener {
            val navController = findNavController()
            navController.navigate(R.id.action_profileFragment_to_settingsFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateVisitedLocationsProgress(visited: Int, total: Int) {
        binding.txtProgressLabel.text =
            "Has descubierto $visited de $total lugares secretos"
        binding.progressVisited.max = total
        binding.progressVisited.progress = visited
    }

    private fun showUserProfile(user: User) {
        binding.txtUsername.text = user.displayName
        binding.txtBio.text = user.bio
    }

    private fun populateLocationsGrid(
        allLocations: List<Location>,
        visitedLocations: List<Location>
    ) {
        binding.gridVisitedLocations.removeAllViews()

        val imageMargin = 20
        val imageTargetSize = 200

        val visitedLocationIds = visitedLocations.map { it.id }.toSet()

        val colorMatrix = ColorMatrix().apply {
            setSaturation(0f)
        }
        val grayscaleFilter = ColorMatrixColorFilter(colorMatrix)

        allLocations.forEach { location ->
            val isVisited = visitedLocationIds.contains(location.id)

            val imageView = ImageView(requireContext()).apply {
                val params = GridLayout.LayoutParams().apply {
                    width = GridLayout.LayoutParams.WRAP_CONTENT
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    setMargins(imageMargin)
                }
                layoutParams = params
            }

            Glide.with(this@ProfileFragment)
                .load(location.imageUrl)
                .override(imageTargetSize, imageTargetSize)
                .centerCrop()
                .circleCrop()
                .into(imageView)

            if (!isVisited) {
                imageView.colorFilter = grayscaleFilter
            } else {
                imageView.clearColorFilter()
            }

            binding.gridVisitedLocations.addView(imageView)
        }
    }
}
