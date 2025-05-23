package dadm.grupo.dadmproyecto.ui.destinationmap

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.databinding.FragmentDestinationMapBinding
import dadm.grupo.dadmproyecto.utils.LoadImageUtils.createCircularBitmapFromBitmap
import dadm.grupo.dadmproyecto.utils.LoadImageUtils.loadBitmapFromUrl
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
    private var isAnimatingToUserLocation = false // Flag to track centering animation

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
        setupCameraAndBounds(mapLibreMap) // Sets initial position (e.g., UPV)

        // If the map should be centered on the user, immediately move the camera
        // to the last known location without animation to prevent the initial jump.
        if (viewModel.isMapCenteredInUserLocation.value) {
            viewModel.lastKnownLocation.value?.let { location ->
                val userLatLng = LatLng(location.latitude, location.longitude)
                mapLibreMap.cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
                    .target(userLatLng)
                    .zoom(MAP_ZOOM_LEVEL) // Use the same zoom level
                    .build()
                Log.d(
                    "CenterLocation",
                    "onMapReady: Immediately centering on last known user location."
                )
            } ?: run {
                Log.d("CenterLocation", "onMapReady: Centering enabled, but last location is null.")
            }
        }

        // Setup map interaction listeners here, after the map is ready
        setupMapListeners(mapLibreMap)
        // Setup marker click listener (it will handle clicks if markers exist)
        setupMarkerClickListener(markerOriginalBitmaps)

        val prefs = requireContext().getSharedPreferences("map_prefs", Context.MODE_PRIVATE)


        //prefs.edit { putBoolean("has_seen_tutorial", false) }


        val hasSeenTutorial = prefs.getBoolean("has_seen_tutorial", false)
        Log.d("DestinationMapFragment", "Has seen tutorial: $hasSeenTutorial")

        if (!hasSeenTutorial) {
            startTutorial()
            prefs.edit { putBoolean("has_seen_tutorial", true) }
        }
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

        // This sets the default position if not immediately overridden by user centering logic above
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
        observeMapIsCenteredInUserLocation()

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

        binding.fabCenterLocation.setOnClickListener {
            Log.d("CenterLocation", "FAB clicked")
            viewModel.changeMapCenterInUserLocation()
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
                            "Adding ${newLocations.size} new visited locations concurrently"
                        )

                        // Launch a new coroutine to handle concurrent loading and marker addition
                        viewLifecycleOwner.lifecycleScope.launch {
                            val markerDataDeferred = newLocations.map { location ->
                                // Launch concurrent bitmap loading on IO dispatcher
                                async(kotlinx.coroutines.Dispatchers.IO) {
                                    val bitmap = loadBitmapFromUrl(location.imageUrl)
                                    // Prepare data needed for marker creation
                                    Triple(
                                        location,
                                        LatLng(location.latitude, location.longitude),
                                        bitmap
                                    )
                                }
                            }

                            // Await all loading tasks to complete
                            val markerDataResults = markerDataDeferred.awaitAll()

                            // Switch back to the Main thread to update the UI (add markers)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                markerDataResults.forEach { (location, latLng, bitmap) ->
                                    bitmap?.let { loadedBitmap ->
                                        val circularBitmap =
                                            createCircularBitmapFromBitmap(
                                                loadedBitmap,
                                                targetSize = 125
                                            )
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
                                            Log.d(
                                                "DestinationMapFragment",
                                                "Added marker for: ${location.name}"
                                            )
                                        }
                                    } ?: Log.w(
                                        "DestinationMapFragment",
                                        "Failed to load bitmap for: ${location.name}"
                                    )
                                }
                                Log.d(
                                    "DestinationMapFragment",
                                    "Finished adding markers for new locations."
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    private fun setupMapListeners(map: MapLibreMap) {
        map.addOnMapClickListener { point ->
            Log.d("CenterLocation", "Map clicked at: $point")
            isAnimatingToUserLocation = false // Stop tracking animation on map click
            viewModel.setMapCenterInUserLocation(false)

            // Also handle deselecting marker on map click
            selectedMarker?.let {
                restoreMarkerIcon(it, markerOriginalBitmaps)
                it.hideInfoWindow()
                selectedMarker = null
            }
            true // Indicate the click was handled
        }

        map.addOnCameraMoveListener {
            // Only set centered to false if the move wasn't caused by our animation
            if (!isAnimatingToUserLocation) {
                viewModel.setMapCenterInUserLocation(false)
                Log.d("CenterLocation", "Camera moved manually")
            }

            // If a marker is selected, deselect it when the map moves
            selectedMarker?.let { marker ->
                Log.d("MarkerDeselect", "Map moved, deselecting marker: ${marker.title}")
                restoreMarkerIcon(marker, markerOriginalBitmaps)
                marker.hideInfoWindow()
                selectedMarker = null
            }
        }
    }

    private fun setupMarkerClickListener(
        markerBitmaps: MutableMap<Marker, Bitmap>
    ) {
        mapLibreMap?.setOnMarkerClickListener { marker ->
            Log.d("MarkerClick", "Marker clicked: ${marker.title}")
            if (this.selectedMarker == marker) {
                // Clicked the same marker again, deselect it
                restoreMarkerIcon(marker, markerBitmaps)
                marker.hideInfoWindow()
                this.selectedMarker = null // Update fragment's property
            } else {
                // Clicked a new marker
                // Deselect the previously selected marker, if any
                this.selectedMarker?.let { prevMarker ->
                    restoreMarkerIcon(prevMarker, markerBitmaps)
                    // No need to hide info window here, showInfoWindow below handles it
                }
                // Select the new marker
                enlargeMarkerIcon(marker, markerBitmaps)
                marker.showInfoWindow(
                    mapLibreMap!!,
                    binding.mapView
                ) // Ensure mapLibreMap is not null here
                this.selectedMarker = marker // Update fragment's property
            }
            true // Indicate the click was handled
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
        binding.tvNewLocationDiscovered.text =
            getString(R.string.new_location_discovered, name)

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
                                mapLibreMap?.locationComponent?.forceLocationUpdate(it)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeMapIsCenteredInUserLocation() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isMapCenteredInUserLocation.collect { isCentered ->
                    if (isCentered) {
                        binding.fabCenterLocation.setImageResource(R.drawable.icon_navigation_pin)
                        if (isCentered) {
                            viewModel.lastKnownLocation.value?.let { location ->
                                val latLng = LatLng(location.latitude, location.longitude)
                                val currentTarget = mapLibreMap?.cameraPosition?.target
                                val distance = currentTarget?.distanceTo(latLng) ?: Double.MAX_VALUE

                                if (!isAnimatingToUserLocation && distance > 10) { // Threshold distance in meters
                                    Log.d(
                                        "CenterLocation",
                                        "observeMapIsCenteredInUserLocation: Animating to user location."
                                    )
                                    isAnimatingToUserLocation =
                                        true // Set flag before starting animation
                                    mapLibreMap?.animateCamera(
                                        org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                                            latLng,
                                            MAP_ZOOM_LEVEL
                                        ),
                                        object : MapLibreMap.CancelableCallback {
                                            override fun onCancel() {
                                                isAnimatingToUserLocation = false
                                                Log.d(
                                                    "CenterLocation",
                                                    "Animation cancelled, flag reset"
                                                )
                                            }

                                            override fun onFinish() {
                                                isAnimatingToUserLocation = false
                                                Log.d(
                                                    "CenterLocation",
                                                    "Animation finished, flag reset"
                                                )
                                            }
                                        }
                                    )
                                } else {
                                    Log.d(
                                        "CenterLocation",
                                        "observeMapIsCenteredInUserLocation: Already centered or animating, skipping animation."
                                    )
                                }
                            } ?: run {
                                Log.d(
                                    "CenterLocation",
                                    "observeMapIsCenteredInUserLocation: Centering enabled, but last location is null."
                                )
                            }
                        }
                    } else {
                        binding.fabCenterLocation.setImageResource(R.drawable.icon_navigation_off)
                        isAnimatingToUserLocation = false
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
        isAnimatingToUserLocation = false
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
        isAnimatingToUserLocation = false // Reset flag on destroy
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
        val orientation = resources.configuration.orientation
        val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE

        Log.d("DebugOrientation", "Orientation: $orientation, Is Landscape: $isLandscape")

        val translationValue = 100f // Distance to translate

        if (isOpen) {
            binding.fabStyleStandard.show()
            binding.fabStyleSatellite.show()

            binding.fabStyleStandard.alpha = 0f
            binding.fabStyleSatellite.alpha = 0f

            if (isLandscape) {
                binding.fabStyleStandard.translationX = translationValue
                binding.fabStyleSatellite.translationX = translationValue

                binding.fabStyleStandard.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(300)
                    .start()

                binding.fabStyleSatellite.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(300)
                    .setStartDelay(50)
                    .start()
            } else { // Portrait
                binding.fabStyleStandard.translationY = translationValue
                binding.fabStyleSatellite.translationY = translationValue

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
            }
        } else { // Closing animation
            if (isLandscape) {
                binding.fabStyleStandard.animate()
                    .alpha(0f)
                    .translationX(translationValue)
                    .setDuration(300)
                    .start()

                binding.fabStyleSatellite.animate()
                    .alpha(0f)
                    .translationX(translationValue)
                    .setDuration(300)
                    .setStartDelay(50)
                    .withEndAction {
                        binding.fabStyleStandard.hide()
                        binding.fabStyleSatellite.hide()
                        // Reset translation for next time
                        binding.fabStyleStandard.translationX = 0f
                        binding.fabStyleSatellite.translationX = 0f
                    }
                    .start()
            } else { // Portrait
                binding.fabStyleStandard.animate()
                    .alpha(0f)
                    .translationY(translationValue)
                    .setDuration(300)
                    .start()

                binding.fabStyleSatellite.animate()
                    .alpha(0f)
                    .translationY(translationValue)
                    .setDuration(300)
                    .setStartDelay(50)
                    .withEndAction {
                        binding.fabStyleStandard.hide()
                        binding.fabStyleSatellite.hide()
                        // Reset translation for next time
                        binding.fabStyleStandard.translationY = 0f
                        binding.fabStyleSatellite.translationY = 0f
                    }
                    .start()
            }
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

    companion object {
        private const val ID_MAP_STYLE_FAB = 100
        private const val ID_MAP_STYLE_SATELLITE = 101
        private const val ID_MAP_STYLE_STANDARD = 102
        private const val ID_MAP_CENTER_LOCATION = 103

    }

    private fun startTutorial() {
        // Define the bounds for the first tap target (center of the screen)
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        val targetRadius = 100 // Adjust radius as needed
        val targetBounds = Rect(
            centerX - targetRadius,
            centerY - targetRadius,
            centerX + targetRadius,
            centerY + targetRadius
        )

        val sequence = TapTargetSequence(requireActivity())
            .targets(
                TapTarget.forBounds(
                    targetBounds,
                    getString(R.string.map_tutorial_title),
                    getString(R.string.map_tutorial_description)
                )
                    .id(0)
                    .textColor(android.R.color.white)
                    .transparentTarget(true) // deja ver el botón exactamente igual
                    .targetCircleColor(android.R.color.transparent) // sin color sobre el botón
                    .cancelable(false), // FAB principal

                TapTarget.forView(
                    binding.fabMain,
                    getString(R.string.map_styles_title),
                    getString(R.string.map_styles_description)
                )
                    .id(ID_MAP_STYLE_FAB)
                    .transparentTarget(true) // deja ver el botón exactamente igual
                    .targetCircleColor(android.R.color.transparent) // sin color sobre el botón
                    .textColor(android.R.color.white)
                    .cancelable(false), // FAB principal

                TapTarget.forView(
                    binding.fabStyleSatellite,
                    getString(R.string.satellite_style_title),
                    getString(R.string.satellite_style_description)
                )
                    .id(ID_MAP_STYLE_SATELLITE)
                    .transparentTarget(true) // deja ver el botón exactamente igual
                    .targetCircleColor(android.R.color.transparent) // sin color sobre el botón
                    .textColor(android.R.color.white)
                    .cancelable(false), // FAB estilo satélite

                TapTarget.forView(
                    binding.fabStyleStandard,
                    getString(R.string.standard_style_title),
                    getString(R.string.standard_style_description)
                )
                    .id(ID_MAP_STYLE_STANDARD)
                    .transparentTarget(true) // deja ver el botón exactamente igual
                    .targetCircleColor(android.R.color.transparent) // sin color sobre el botón
                    .textColor(android.R.color.white)
                    .cancelable(false), // FAB estilo estándar
                TapTarget.forView(
                    binding.fabCenterLocation,
                    getString(R.string.current_location_title),
                    getString(R.string.current_location_description)
                )
                    .id(ID_MAP_CENTER_LOCATION)
                    .transparentTarget(true) // deja ver el botón exactamente igual
                    .targetCircleColor(android.R.color.transparent) // sin color sobre el botón
                    .textColor(android.R.color.white)
                    .cancelable(false) // FAB ubicación actual
            )
            .listener(object : TapTargetSequence.Listener {
                override fun onSequenceFinish() {
                    Log.d("DestinationMapFragment", "Tutorial completed")
                }

                override fun onSequenceStep(currentTarget: TapTarget, targetClicked: Boolean) {
                    Log.d(
                        "DestinationMapFragment",
                        "Tutorial step completed: $currentTarget, Clicked: $targetClicked"
                    )
                    if (targetClicked && currentTarget.id() == ID_MAP_STYLE_FAB) {
                        binding.fabMain.performClick()
                    }

                    if (targetClicked && currentTarget.id() == ID_MAP_STYLE_SATELLITE) {
                        binding.fabStyleSatellite.performClick()
                        binding.fabMain.performClick()
                    }

                    if (targetClicked && currentTarget.id() == ID_MAP_STYLE_STANDARD) {
                        binding.fabStyleStandard.performClick()
                    }

                    if (targetClicked && currentTarget.id() == ID_MAP_CENTER_LOCATION) {
                        binding.fabCenterLocation.performClick()
                    }

                }

                override fun onSequenceCanceled(lastTarget: TapTarget) {
                    Log.d("DestinationMapFragment", "Tutorial canceled at: $lastTarget")
                }
            })

        sequence.start()
    }
}
