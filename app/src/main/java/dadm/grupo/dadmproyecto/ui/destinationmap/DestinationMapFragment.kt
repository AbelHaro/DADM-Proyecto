package dadm.grupo.dadmproyecto.ui.destinationmap

import android.annotation.SuppressLint
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.modes.CameraMode
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

        // Inicializa MapLibre con la configuración predeterminada
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

    // Callback cuando el mapa está listo
    @SuppressLint("MissingPermission")
    override fun onMapReady(mapLibreMap: MapLibreMap) {
        this.mapLibreMap = mapLibreMap
        mapLibreMap.setStyle(
            Style.Builder().fromJson(viewModel.mapStyle.value) // Establece el estilo del mapa
        ) { style ->

            val locationComponent = mapLibreMap.locationComponent

            if (viewModel.isLocationPermissionGranted.value) {
                val locationComponentOptions = LocationComponentOptions.builder(requireContext())
                    .pulseEnabled(true)
                    .build()

                val locationComponentActivationOptions =
                    buildLocationComponentActivationOptions(style, locationComponentOptions)

                locationComponent.activateLocationComponent(locationComponentActivationOptions)
                locationComponent.isLocationComponentEnabled = true
                locationComponent.cameraMode = CameraMode.TRACKING
            } else {
                Log.d("DestinationMapFragment", "Location permissions not granted")
            }

            // Agrega un marcador en la posición de la UPV
            viewModel.markers.value.forEach { latLng ->
                val markerOptions = MarkerOptions()
                    .snippet("Marker")
                    .position(latLng)
                    .title("Marker")
                Log.d("DestinationMapFragment", "Adding marker at: $latLng")
                mapLibreMap.addMarker(markerOptions)
            }
        }

        // Configura la posición de la cámara en el mapa
        mapLibreMap.cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
            .target(
                org.maplibre.android.geometry.LatLng(
                    viewModel.upvPosition.latitude,
                    viewModel.upvPosition.longitude
                )
            )
            .zoom(MAP_ZOOM_LEVEL) // Establece el nivel de zoom
            .bearing(MAP_BEARING_ANGLE) // Dirección de la cámara
            .tilt(MAP_TILT_ANGLE) // Inclinación de la cámara
            .build()

        // Establece los límites de zoom y el área visible
        mapLibreMap.setMinZoomPreference(MAP_MIN_ZOOM)
        mapLibreMap.setMaxZoomPreference(MAP_MAX_ZOOM)
        val boundsBuilder = org.maplibre.android.geometry.LatLngBounds.Builder()
            .include(
                org.maplibre.android.geometry.LatLng(
                    viewModel.upvPosition.latitude + MAP_LATITUDE_OFFSET,
                    viewModel.upvPosition.longitude + MAP_LONGITUDE_OFFSET
                )
            )
            .include(
                org.maplibre.android.geometry.LatLng(
                    viewModel.upvPosition.latitude - MAP_LATITUDE_OFFSET,
                    viewModel.upvPosition.longitude - MAP_LONGITUDE_OFFSET
                )
            )
        mapLibreMap.setLatLngBoundsForCameraTarget(boundsBuilder.build()) // Define los límites del mapa
    }

    // Función para construir las opciones de activación del LocationComponent
    private fun buildLocationComponentActivationOptions(
        style: Style,
        locationComponentOptions: LocationComponentOptions
    ): LocationComponentActivationOptions {
        return LocationComponentActivationOptions
            .builder(requireContext(), style)
            .locationComponentOptions(locationComponentOptions)
            .useDefaultLocationEngine(true) // Usa el motor de ubicación predeterminado
            .locationEngineRequest(
                LocationEngineRequest.Builder(750)
                    .setFastestInterval(750) // Intervalo más rápido para obtener ubicación
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY) // Alta precisión
                    .build()
            )
            .build()
    }

    // Observa los eventos de navegación desde el ViewModel
    private fun observeNavigationEvents() {
//        viewLifecycleOwner.lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                viewModel.navigationEvent.collect { event ->
//                    when (event) {
//                        is DestinationMapViewModel.NavigationEvent.NavigateToAuth -> navigateToAuth()
//                    }
//                }
//            }
//        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isFabMenuOpenState.collect { isMenuOpen ->
                    binding.fabStyleStandard.visibility =
                        if (isMenuOpen) View.VISIBLE else View.INVISIBLE
                    binding.fabStyleSatellite.visibility =
                        if (isMenuOpen) View.VISIBLE else View.INVISIBLE
                }
            }
        }


        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mapStyle.collect { style ->
                    mapLibreMap?.setStyle(
                        Style.Builder().fromJson(style)
                    )
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLocationPermissionGranted.collect { isGranted ->
                    if (isGranted) {
                        binding.mapView.getMapAsync { map ->
                            val locationComponent = map.locationComponent
                            val locationComponentOptions =
                                LocationComponentOptions.builder(requireContext())
                                    .pulseEnabled(true)
                                    .build()

                            val locationComponentActivationOptions =
                                buildLocationComponentActivationOptions(
                                    map.style!!,
                                    locationComponentOptions
                                )

                            locationComponent.activateLocationComponent(
                                locationComponentActivationOptions
                            )
                            locationComponent.isLocationComponentEnabled = true
                        }
                    } else {
                        Log.d("DestinationMapFragment", "Location permissions not granted")
                    }

                }
            }
        }


        binding.fabMain.setOnClickListener {
            viewModel.toggleFabMenu()
        }

        binding.fabStyleStandard.setOnClickListener {
            viewModel.changeMapStyle(STANDARD_MAP_STYLE)
            viewModel.toggleFabMenu()
        }

        binding.fabStyleSatellite.setOnClickListener {
            viewModel.changeMapStyle(SATELLITE_MAP_STYLE)
            viewModel.toggleFabMenu()
        }
    }

//    private fun navigateToAuth() {
//        Intent(requireActivity(), AuthActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        }.also { intent ->
//            startActivity(intent)
//            requireActivity().finish()
//        }
//    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()

        if (viewModel.isLocationPermissionGranted.value) {
            binding.mapView.getMapAsync @androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION]) { map ->
                val locationComponent = map.locationComponent
                val locationComponentOptions = LocationComponentOptions.builder(requireContext())
                    .pulseEnabled(true)
                    .build()

                val locationComponentActivationOptions =
                    buildLocationComponentActivationOptions(map.style!!, locationComponentOptions)

                locationComponent.activateLocationComponent(locationComponentActivationOptions)
                locationComponent.isLocationComponentEnabled = true
                //locationComponent.cameraMode = CameraMode.TRACKING
            }
        }
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
