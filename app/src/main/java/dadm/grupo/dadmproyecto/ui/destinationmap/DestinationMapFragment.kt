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
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
import org.maplibre.android.location.modes.RenderMode
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
                locationComponent.renderMode = RenderMode.COMPASS
            } else {
                Log.d("DestinationMapFragment", "Location permissions not granted")
            }

            // Agrega un marcador en la posición de la UPV
            viewModel.markers.value.forEach { marker ->
                val latLng = org.maplibre.android.geometry.LatLng(
                    marker.latitude,
                    marker.longitude
                )

                val markerOptions = MarkerOptions()
                    .snippet("Marker")
                    .position(latLng)
                    .title(marker.name)
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
                        binding.mapView.getMapAsync @androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION]) { map ->
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

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.markers.collect { markers ->
                    mapLibreMap?.removeAnnotations()
                    markers.forEach { marker ->
                        val latLng = org.maplibre.android.geometry.LatLng(
                            marker.latitude,
                            marker.longitude
                        )

                        val markerOptions = MarkerOptions()
                            .snippet("Marker")
                            .position(latLng)
                            .title(marker.name)
                        Log.d("DestinationMapFragment", "Adding marker at: $latLng")
                        mapLibreMap?.addMarker(markerOptions)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isFabMenuOpenState.collect { isMenuOpen ->
                    animateMenuOpen(isMenuOpen)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLocationPermissionGranted.collect { isGranted ->
                    if (isGranted) {
                        Log.d("DestinationMapFragment", "Location permissions granted")
                        binding.mapView.getMapAsync @androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION]) { map ->
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

                    // Monitoriza los cambios de posicion para actualizar la UI en tiempo real
                    viewModel.getAccurateLocationUpdates().collect { location ->
                        location?.let {
                            val currentLatLng = org.maplibre.android.geometry.LatLng(
                                it.latitude,
                                it.longitude
                            )
                            if (mapLibreMap != null) {
                                mapLibreMap!!.locationComponent.forceLocationUpdate(it)
                                mapLibreMap!!.animateCamera(
                                    org.maplibre.android.camera.CameraUpdateFactory.newLatLng(
                                        currentLatLng
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        setupFabAnimations()

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

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()

        viewModel.checkLocationPermission()

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


    // In DestinationMapFragment.kt

    private fun setupFabAnimations() {
        // Setup click listeners with animations
        binding.fabMain.setOnClickListener {
            // Animate the main FAB rotation
            val rotationAngle = if (viewModel.isFabMenuOpenState.value) 0f else 45f
            binding.fabMain.animate()
                .rotation(rotationAngle)
                .setDuration(300)
                .start()

            viewModel.toggleFabMenu()
        }

        // Secondary FABs with animations
        binding.fabStyleStandard.setOnClickListener {
            animateFabClick(binding.fabStyleStandard)
            viewModel.changeMapStyle(STANDARD_MAP_STYLE)
            viewModel.toggleFabMenu()
        }

        binding.fabStyleSatellite.setOnClickListener {
            animateFabClick(binding.fabStyleSatellite)
            viewModel.changeMapStyle(SATELLITE_MAP_STYLE)
            viewModel.toggleFabMenu()
        }
    }

    private fun animateMenuOpen(isOpen: Boolean) {
        if (isOpen) {
            binding.fabStyleStandard.show()
            binding.fabStyleSatellite.show()

            binding.fabStyleStandard.alpha = 0f
            binding.fabStyleSatellite.alpha = 0f

            binding.fabStyleStandard.translationY = 100f
            binding.fabStyleSatellite.translationY = 100f

            binding.fabStyleStandard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .start()

            binding.fabStyleSatellite.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay(50)
                .start()
        } else {
            binding.fabStyleStandard.animate()
                .alpha(0f)
                .translationY(100f)
                .setDuration(300)
                .start()

            binding.fabStyleSatellite.animate()
                .alpha(0f)
                .translationY(100f)
                .setDuration(300)
                .setStartDelay(50)
                .withEndAction {
                    binding.fabStyleStandard.hide()
                    binding.fabStyleSatellite.hide()
                }
                .start()
        }
    }

    private fun animateFabClick(fab: FloatingActionButton) {
        fab.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(100)
            .withEndAction {
                fab.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }
}
