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
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style

@AndroidEntryPoint
class DestinationMapFragment : Fragment(), OnMapReadyCallback {

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

        // Inicializar el mapa
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        setupObservers()
    }

    override fun onMapReady(map: MapLibreMap) {
        mapLibreMap = map
        map.setStyle(
            Style.Builder().fromUri("https://tiles.openfreemap.org/styles/liberty")

        ) { style ->
            setupMapControls()
            loadMarkers()
        }

        map.cameraPosition =
            CameraPosition.Builder().target(LatLng(39.481096, -0.341373)).zoom(10.0).build()

        val bounds = LatLngBounds.Builder()
            .include(LatLng(39.481096, -0.341373))
            .include(LatLng(39.492096, -0.340373))
            .build()

        map.getCameraForLatLngBounds(bounds)?.let {
            map.cameraPosition = it
        }
    }

    private fun setupMapControls() {
        mapLibreMap?.uiSettings?.apply {
            isCompassEnabled = true
            isZoomGesturesEnabled = true
            isScrollGesturesEnabled = true
        }
    }

    private fun loadMarkers() {
        viewModel.markers.value.forEach { latLng ->
            mapLibreMap?.addMarker(
                org.maplibre.android.annotations.MarkerOptions()
                    .position(latLng)
                    .title("Marcador")
            )
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {


                launch {
                    viewModel.navigationEvent.collect { event ->
                        if (event is DestinationMapViewModel.NavigationEvent.NavigateToAuth) {
                            navigateToAuth()
                        }
                    }
                }
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.onDestroy()
        _binding = null
        mapLibreMap = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }
}
