package dadm.grupo.dadmproyecto.ui.destinationmap

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dadm.grupo.dadmproyecto.databinding.FragmentDestinationMapBinding
import dadm.grupo.dadmproyecto.ui.AuthActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.maps.MapLibreMap

@AndroidEntryPoint
class DestinationMapFragment : Fragment() {

    private var _binding: FragmentDestinationMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DestinationMapViewModel by viewModels()
    private var mapLibreMap: MapLibreMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(requireContext(), null, WellKnownTileServer.MapLibre)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDestinationMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeMap(savedInstanceState)
        setupObservers()
        setupListeners()
    }

    private fun initializeMap(savedInstanceState: Bundle?) {
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync { map ->
            mapLibreMap = map
            setupMapControls()
            setupMapListeners()
            observeMapData()
        }
    }

    private fun setupMapControls() {
        mapLibreMap?.uiSettings?.apply {
            isCompassEnabled = true
            isRotateGesturesEnabled = true
        }
    }

    private fun setupMapListeners() {
        mapLibreMap?.addOnMapClickListener { point ->
            viewModel.addMarker(LatLng(point.latitude, point.longitude))
            true
        }
    }

    private fun observeMapData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.centerMap.collect { center ->
                        center?.let {
                            mapLibreMap?.camera?.move(
                                CameraPosition.Builder()
                                    .target(it)
                                    .zoom(12.0)
                                    .build()
                            )
                        }
                    }
                }

                launch {
                    viewModel.markers.collect { markers ->
                        // Limpiar marcadores existentes y añadir los nuevos
                        binding.mapView.annotations?.let { annotations ->
                            AnnotationPluginImplKt.getAnnotations(annotations)
                                .pointAnnotationManager?.deleteAll()

                            markers.forEach { position ->
                                addMarkerToMap(position)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun addMarkerToMap(position: LatLng) {
        val annotationApi = binding.mapView.annotations
        val pointAnnotationManager = AnnotationPluginImplKt.getAnnotations(annotationApi)
            .let { PointAnnotationManagerKt.createPointAnnotationManager(it, binding.mapView) }

        val markerIcon = BitmapFactory.decodeResource(
            resources,
            R.drawable.ic_map_marker
        )

        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(Point.fromLngLat(position.longitude, position.latitude))
            .withIconImage(markerIcon)

        pointAnnotationManager.create(pointAnnotationOptions)
    }

    // Métodos del ciclo de vida del MapView
    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.onDestroy()
        _binding = null
        mapLibreMap = null
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userData.collect { user ->
                        user?.let {
                            binding.tvFragmentDestinationMap.text = buildString {
                                appendLine("Información del usuario:")
                                appendLine("Email: ${viewModel.getUserEmail()}")
                                appendLine("Nombre: ${viewModel.getUserDisplayName()}")
                                appendLine("ID: ${viewModel.getUserId()}")
                            }
                        }
                    }
                }

                launch {
                    viewModel.errorMessage.collect { error ->
                        error?.let {
                            binding.tvFragmentDestinationMap.text = it
                        }
                    }
                }

                launch {
                    viewModel.navigationEvent.collect { event ->
                        when (event) {
                            is DestinationMapViewModel.NavigationEvent.NavigateToAuth -> {
                                navigateToAuth()
                            }
                        }
                    }
                }

                launch {
                    viewModel.showConfirmationDialog.collect { show ->
                        if (show) {
                            showLogoutConfirmationDialog()
                        }
                    }
                }
            }
        }
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                viewModel.confirmLogout()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                viewModel.dismissLogoutConfirmation()
            }
            .show()
    }

    private fun setupListeners() {
        binding.btnLogout.setOnClickListener {
            viewModel.showLogoutConfirmation()
        }
    }

    private fun navigateToAuth() {
        Intent(requireActivity(), AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }.also { intent ->
            startActivity(intent)
            requireActivity().finish()
        }
    }
}
