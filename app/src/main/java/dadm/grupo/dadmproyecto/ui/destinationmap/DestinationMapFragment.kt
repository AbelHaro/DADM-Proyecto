package dadm.grupo.dadmproyecto.ui.destinationmap

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDestinationMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)
        observeNavigationEvents()
    }

    override fun onMapReady(map: MapLibreMap) {
        mapLibreMap = map

        map.setStyle(Style.Builder().fromJson(viewModel.mapStyle.value)) { style ->

//Quitar los marcadores que vienen por defecto
//            style.layers.forEach { layer ->
//                val layerId = layer.id
//                if (layerId.contains("label") || layerId.contains("place") ||
//                    layerId.contains("poi") || layerId.contains("name")
//                ) {
//                    style.getLayer(layerId)?.setProperties(
//                        org.maplibre.android.style.layers.PropertyFactory.visibility("none")
//                    )
//                }
//            }


            // Agrega los marcadores que vienen del ViewModel
            viewModel.markers.value.forEach { latLng ->
                val markerOptions = org.maplibre.android.annotations.MarkerOptions()
                    .snippet("Marker")
                    .position(latLng)
                    .title("Marker")

                Log.d("DestinationMapFragment", "Adding marker at: $latLng")

                mapLibreMap?.addMarker(markerOptions) ?: run {
                    Log.e("DestinationMapFragment", "Cannot add marker - mapLibreMap is null")
                }
            }


        }



        map.cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
            .target(
                org.maplibre.android.geometry.LatLng(
                    viewModel.UPV_POSITION.latitude,
                    viewModel.UPV_POSITION.longitude
                )
            )
            .zoom(15.0)
            .bearing(20.7)
            .tilt(20.0)
            .build()

        map.setMinZoomPreference(15.0)
        map.setMaxZoomPreference(20.0)

        // Set bounds to limit map movement
        val boundsBuilder = org.maplibre.android.geometry.LatLngBounds.Builder()
            .include(
                org.maplibre.android.geometry.LatLng(
                    viewModel.UPV_POSITION.latitude + 0.02,
                    viewModel.UPV_POSITION.longitude + 0.02
                )
            )
            .include(
                org.maplibre.android.geometry.LatLng(
                    viewModel.UPV_POSITION.latitude - 0.02,
                    viewModel.UPV_POSITION.longitude - 0.02
                )
            )
        map.setLatLngBoundsForCameraTarget(boundsBuilder.build())


    }

    private fun observeNavigationEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvent.collect { event ->
                    when (event) {
                        is DestinationMapViewModel.NavigationEvent.NavigateToAuth -> navigateToAuth()
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.onDestroy()
        _binding = null
        mapLibreMap = null
    }
}
