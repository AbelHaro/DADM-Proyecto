package dadm.grupo.dadmproyecto.ui.destinationmap

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dadm.grupo.dadmproyecto.databinding.FragmentDestinationMapBinding
import dadm.grupo.dadmproyecto.utils.LoadImageUtils.createCircularBitmapFromBitmap
import dadm.grupo.dadmproyecto.utils.LoadImageUtils.loadBitmapFromUrl
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponent
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
        setupMapStyle(mapLibreMap)
        setupCameraAndBounds(mapLibreMap)
    }


    private fun setupMapStyle(map: MapLibreMap) {
        map.setStyle(Style.Builder().fromJson(viewModel.mapStyle.value)) { style ->
            if (viewModel.isLocationPermissionGranted.value) {
                enableLocationComponent(map.locationComponent, style)
            } else {
                Log.d("DestinationMapFragment", "Location permissions not granted")
            }
        }
    }

    private fun setupCameraAndBounds(map: MapLibreMap) {
        val upvLat = viewModel.upvPosition.latitude
        val upvLng = viewModel.upvPosition.longitude

        map.cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
            .target(LatLng(upvLat, upvLng))
            .zoom(MAP_ZOOM_LEVEL)
            .bearing(MAP_BEARING_ANGLE)
            .tilt(MAP_TILT_ANGLE)
            .build()

        map.setMinZoomPreference(MAP_MIN_ZOOM)
        map.setMaxZoomPreference(MAP_MAX_ZOOM)

        val boundsBuilder = org.maplibre.android.geometry.LatLngBounds.Builder()
            .include(LatLng(upvLat + MAP_LATITUDE_OFFSET, upvLng + MAP_LONGITUDE_OFFSET))
            .include(LatLng(upvLat - MAP_LATITUDE_OFFSET, upvLng - MAP_LONGITUDE_OFFSET))

        map.setLatLngBoundsForCameraTarget(boundsBuilder.build())
    }

    private fun enableLocationComponent(component: LocationComponent, style: Style) {
        val options = LocationComponentOptions.builder(requireContext())
            .pulseEnabled(true)
            .build()

        val activationOptions = buildLocationComponentActivationOptions(style, options)
        component.activateLocationComponent(activationOptions)

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Safe to enable the component as we've checked permissions
            component.isLocationComponentEnabled = true
            component.cameraMode = CameraMode.TRACKING
            component.renderMode = RenderMode.COMPASS
        }
    }

    private fun buildLocationComponentActivationOptions(
        style: Style,
        locationComponentOptions: LocationComponentOptions
    ): LocationComponentActivationOptions {
        return LocationComponentActivationOptions.builder(requireContext(), style)
            .locationComponentOptions(locationComponentOptions)
            .useDefaultLocationEngine(true)
            .locationEngineRequest(
                LocationEngineRequest.Builder(750)
                    .setFastestInterval(750)
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                    .build()
            )
            .build()
    }


    private fun observeNavigationEvents() {
        observeFabMenuState()
        observeMapStyle()
        observeLocationPermission()
        observeVisitedLocations()
        observeLastDiscoveredLocation()
        observeAccurateLocationUpdates()

        setupFabAnimations()

        binding.fabMain.setOnClickListener { viewModel.toggleFabMenu() }
        binding.fabStyleStandard.setOnClickListener {
            viewModel.changeMapStyle(STANDARD_MAP_STYLE)
            viewModel.toggleFabMenu()
        }
        binding.fabStyleSatellite.setOnClickListener {
            viewModel.changeMapStyle(SATELLITE_MAP_STYLE)
            viewModel.toggleFabMenu()
        }
    }

    private fun observeFabMenuState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isFabMenuOpenState.collect { isMenuOpen ->
                    binding.fabStyleStandard.visibility =
                        if (isMenuOpen) View.VISIBLE else View.INVISIBLE
                    binding.fabStyleSatellite.visibility =
                        if (isMenuOpen) View.VISIBLE else View.INVISIBLE
                    animateMenuOpen(isMenuOpen)
                }
            }
        }
    }

    private fun observeMapStyle() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mapStyle.collect { style ->
                    mapLibreMap?.setStyle(Style.Builder().fromJson(style))
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun observeLocationPermission() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLocationPermissionGranted.collect { isGranted ->
                    if (isGranted) {
                        Log.d("DestinationMapFragment", "Location permissions granted")
                        setupLocationComponent()
                    } else {
                        Log.d("DestinationMapFragment", "Location permissions not granted")
                    }
                }
            }
        }
    }

    @androidx.annotation.RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun setupLocationComponent() {
        binding.mapView.getMapAsync { map ->
            val locationComponent = map.locationComponent
            val locationComponentOptions = LocationComponentOptions.builder(requireContext())
                .pulseEnabled(true)
                .build()

            val activationOptions =
                buildLocationComponentActivationOptions(map.style!!, locationComponentOptions)

            locationComponent.activateLocationComponent(activationOptions)
            locationComponent.isLocationComponentEnabled = true
        }
    }

    private val addedLocationIds = mutableSetOf<Long>()
    private val markerOriginalBitmaps = mutableMapOf<Marker, Bitmap>()
    private var selectedMarker: Marker? = null

    private fun observeVisitedLocations() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.visitedLocations.collect { visitedLocations ->
                    // Find only new locations that haven't been added to the map yet
                    val newLocations = visitedLocations.filter { location ->
                        !addedLocationIds.contains(location.id)
                    }

                    Log.d(
                        "DestinationMapFragment",
                        "Visited locations: ${visitedLocations.size}, New locations: ${newLocations.size}"
                    )


                    if (newLocations.isNotEmpty()) {
                        Log.d(
                            "DestinationMapFragment",
                            "Adding ${newLocations.size} new visited locations"
                        )

                        // Add markers only for new locations
                        newLocations.forEach { location ->
                            val latLng = LatLng(location.latitude, location.longitude)
                            loadBitmapFromUrl(location.imageUrl)?.let { bitmap ->
                                val circularBitmap =
                                    createCircularBitmapFromBitmap(bitmap, targetSize = 125)
                                val icon =
                                    IconFactory.getInstance(requireContext())
                                        .fromBitmap(circularBitmap)

                                val marker = mapLibreMap?.addMarker(
                                    MarkerOptions()
                                        .position(latLng)
                                        .title(location.name)
                                        .snippet(location.description)
                                        .icon(icon)
                                )

                                marker?.let {
                                    markerOriginalBitmaps[it] = circularBitmap
                                    // Track that we've added this location
                                    addedLocationIds.add(location.id)
                                }
                            }
                        }

                        // Only setup the click listener if we have markers
                        if (markerOriginalBitmaps.isNotEmpty()) {
                            setupMarkerClickListener(markerOriginalBitmaps, selectedMarker)
                        }
                    }
                }
            }
        }
    }

    private fun setupMarkerClickListener(
        markerBitmaps: MutableMap<Marker, Bitmap>,
        selected: Marker?
    ) {
        var selectedMarker = selected

        mapLibreMap?.setOnMarkerClickListener { marker ->
            if (selectedMarker == marker) {
                restoreMarkerIcon(marker, markerBitmaps)
                marker.hideInfoWindow()
                selectedMarker = null
            } else {
                selectedMarker?.let { marker ->
                    restoreMarkerIcon(
                        marker,
                        markerBitmaps
                    ).also { marker.hideInfoWindow() }
                }
                enlargeMarkerIcon(marker, markerBitmaps)
                marker.showInfoWindow(mapLibreMap!!, binding.mapView)
                selectedMarker = marker
            }
            true
        }

        mapLibreMap?.addOnMapClickListener {
            selectedMarker?.let {
                restoreMarkerIcon(it, markerBitmaps)
                it.hideInfoWindow()
                selectedMarker = null
            }
            true
        }

        mapLibreMap?.addOnCameraMoveListener {
            selectedMarker?.let { marker ->
                if (marker.isInfoWindowShown) {
                    // Refresh the info window position
                    marker.hideInfoWindow()
                    marker.showInfoWindow(mapLibreMap!!, binding.mapView)
                }
            }
        }
    }

    private fun restoreMarkerIcon(marker: Marker, markerBitmaps: Map<Marker, Bitmap>) {
        markerBitmaps[marker]?.let {
            val icon = IconFactory.getInstance(requireContext()).fromBitmap(it)
            marker.icon = icon
        }
    }

    private fun enlargeMarkerIcon(marker: Marker, markerBitmaps: Map<Marker, Bitmap>) {
        markerBitmaps[marker]?.let {
            val enlargedBitmap = createCircularBitmapFromBitmap(it, targetSize = 250)
            val icon = IconFactory.getInstance(requireContext()).fromBitmap(enlargedBitmap)
            marker.icon = icon
        }
    }

    private fun observeLastDiscoveredLocation() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.lastDiscoveredLocation.collect { location ->
                    if (location == null) return@collect

                    Log.d("DestinationMapFragment", "Last discovered location: $location")
                    loadBitmapFromUrl(location.imageUrl)?.let {
                        val bitmap = createCircularBitmapFromBitmap(it, targetSize = 600)
                        showNewLocationDiscovered(location.name, bitmap)
                    }

                    viewModel.resetLastDiscoveredLocation()
                }
            }
        }
    }

    private fun showNewLocationDiscovered(name: String, bitmap: Bitmap) {
        binding.ivNewLocationDiscovered.setImageBitmap(bitmap)
        binding.tvNewLocationDiscovered.text = "Nueva ubicación descubierta: $name"

        binding.ivNewLocationDiscovered.apply {
            alpha = 0f
            rotationY = 90f
            visibility = View.VISIBLE
        }
        binding.tvNewLocationDiscovered.apply {
            alpha = 0f
            visibility = View.VISIBLE
        }

        binding.ivNewLocationDiscovered.animate()
            .alpha(1f).rotationY(0f).setDuration(700)
            .setInterpolator(DecelerateInterpolator()).start()

        binding.tvNewLocationDiscovered.animate()
            .alpha(1f).setDuration(700)
            .withEndAction {
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(5000)
                    binding.ivNewLocationDiscovered.animate()
                        .alpha(0f).rotationY(-90f).setDuration(700)
                        .setInterpolator(AccelerateInterpolator())
                        .withEndAction {
                            binding.ivNewLocationDiscovered.visibility = View.GONE
                            binding.ivNewLocationDiscovered.rotationY = 90f
                        }
                        .start()
                    binding.tvNewLocationDiscovered.animate()
                        .alpha(0f).setDuration(500)
                        .withEndAction { binding.tvNewLocationDiscovered.visibility = View.GONE }
                        .start()
                }
            }.start()
    }

    private fun observeAccurateLocationUpdates() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLocationPermissionGranted.collect { isGranted ->
                    if (isGranted) {
                        viewModel.getAccurateLocationUpdates().collect { location ->
                            location?.let {
                                Log.d("DestinationMapFragment", "Location updated: $it")
                                val latLng = LatLng(it.latitude, it.longitude)
                                mapLibreMap?.locationComponent?.forceLocationUpdate(it)
                                mapLibreMap?.animateCamera(
                                    org.maplibre.android.camera.CameraUpdateFactory.newLatLng(latLng)
                                )
                            }
                        }
                    }
                }
            }
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
            binding.mapView.getMapAsync @androidx.annotation.RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION]) { map ->
                val locationComponent = map.locationComponent
                val locationComponentOptions = LocationComponentOptions.builder(requireContext())
                    .pulseEnabled(true)
                    .build()

                val locationComponentActivationOptions =
                    buildLocationComponentActivationOptions(map.style!!, locationComponentOptions)

                locationComponent.activateLocationComponent(locationComponentActivationOptions)
                locationComponent.isLocationComponentEnabled = true
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
        addedLocationIds.clear()
        markerOriginalBitmaps.clear()
        selectedMarker = null
        _binding = null
        mapLibreMap = null
    }


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
