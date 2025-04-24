package dadm.grupo.dadmproyecto.ui.destinationmap

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dadm.grupo.dadmproyecto.databinding.FragmentDestinationMapBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import java.net.URL
import javax.net.ssl.HttpsURLConnection


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
        }

        // Configura la posición de la cámara en el mapa
        mapLibreMap.cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
            .target(
                LatLng(
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
                LatLng(
                    viewModel.upvPosition.latitude + MAP_LATITUDE_OFFSET,
                    viewModel.upvPosition.longitude + MAP_LONGITUDE_OFFSET
                )
            )
            .include(
                LatLng(
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
                val markerOriginalBitmaps = mutableMapOf<Marker, Bitmap>()
                var selectedMarker: Marker?

                viewModel.visitedLocations.collect { visitedLocations ->
                    mapLibreMap?.removeAnnotations()
                    markerOriginalBitmaps.clear()
                    selectedMarker = null

                    visitedLocations.forEach { visitedLocation ->
                        val latLng = LatLng(visitedLocation.latitude, visitedLocation.longitude)

                        // Cargar la imagen desde la URL
                        val bitmap = loadBitmapFromUrl(visitedLocation.imageUrl)

                        bitmap?.let {
                            // Redimensionar la imagen a un tamaño fijo de 200x200 píxeles
                            val circularBitmap =
                                createCircularBitmapFromBitmap(it, targetSize = 125) // Tamaño fijo
                            val iconFactory = IconFactory.getInstance(requireContext())
                            val icon = iconFactory.fromBitmap(circularBitmap)

                            val markerOptions = MarkerOptions()
                                .position(latLng)
                                .title(visitedLocation.name)
                                .snippet(visitedLocation.description)
                                .icon(icon)

                            val marker = mapLibreMap?.addMarker(markerOptions)
                            marker?.let { m ->
                                markerOriginalBitmaps[m] = circularBitmap
                            }
                        }
                    }

                    mapLibreMap?.setOnMarkerClickListener { marker ->
                        if (selectedMarker == marker) {
                            // Restaurar tamaño original
                            markerOriginalBitmaps[marker]?.let {
                                val iconFactory = IconFactory.getInstance(requireContext())
                                val icon = iconFactory.fromBitmap(it)
                                marker.icon = icon
                            }
                            marker.hideInfoWindow()  // Esconder la InfoWindow
                            selectedMarker = null
                        } else {
                            // Restaurar el marcador anterior
                            selectedMarker?.let { prevMarker ->
                                markerOriginalBitmaps[prevMarker]?.let {
                                    val iconFactory = IconFactory.getInstance(requireContext())
                                    val icon = iconFactory.fromBitmap(it)
                                    prevMarker.icon = icon
                                }
                                prevMarker.hideInfoWindow()  // Esconder la InfoWindow del anterior
                            }

                            // Ampliar el nuevo marcador
                            markerOriginalBitmaps[marker]?.let { originalBitmap ->
                                val enlargedBitmap =
                                    createCircularBitmapFromBitmap(
                                        originalBitmap,
                                        targetSize = 250
                                    ) // Tamaño ampliado
                                val iconFactory = IconFactory.getInstance(requireContext())
                                val enlargedIcon = iconFactory.fromBitmap(enlargedBitmap)
                                marker.icon = enlargedIcon
                            }

                            marker.showInfoWindow(mapLibreMap!!, binding.mapView)
                            selectedMarker = marker
                        }
                        true
                    }

                    // Esconder la InfoWindow al hacer clic fuera de los marcadores
                    mapLibreMap?.addOnMapClickListener {
                        selectedMarker?.let { marker ->
                            markerOriginalBitmaps[marker]?.let {
                                val iconFactory = IconFactory.getInstance(requireContext())
                                val icon = iconFactory.fromBitmap(it)
                                marker.icon = icon
                            }
                            marker.hideInfoWindow()  // Esconder la InfoWindow
                            selectedMarker = null
                        }
                        true
                    }
                }
            }
        }


        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.lastDiscoveredLocation.collect { lastLocation ->
                    if (lastLocation == null) return@collect

                    Log.d("DestinationMapFragment", "Last discovered location: $lastLocation")

                    val bitmap = loadBitmapFromUrl(lastLocation.imageUrl)

                    bitmap?.let {
                        val circularBitmap = createCircularBitmapFromBitmap(it, targetSize = 600)
                        binding.ivNewLocationDiscovered.setImageBitmap(circularBitmap)
                        binding.tvNewLocationDiscovered.text =
                            "Nueva ubicación descubierta: ${lastLocation.name}"

                        // Preparar vistas
                        binding.ivNewLocationDiscovered.apply {
                            alpha = 0f
                            rotationY = 90f // empieza de perfil, como una moneda
                            visibility = View.VISIBLE
                        }

                        binding.tvNewLocationDiscovered.apply {
                            alpha = 0f
                            visibility = View.VISIBLE
                        }

                        // Animación fade in + giro tipo moneda
                        binding.ivNewLocationDiscovered.animate()
                            .alpha(1f)
                            .rotationY(0f)
                            .setDuration(700)
                            .setInterpolator(DecelerateInterpolator())
                            .start()

                        binding.tvNewLocationDiscovered.animate()
                            .alpha(1f)
                            .setDuration(700)
                            .withEndAction {
                                // Esperar 5 segundos
                                viewLifecycleOwner.lifecycleScope.launch {
                                    delay(5000)

                                    // Animación de salida (fade out + giro inverso)
                                    binding.ivNewLocationDiscovered.animate()
                                        .alpha(0f)
                                        .rotationY(-90f)
                                        .setDuration(700)
                                        .setInterpolator(AccelerateInterpolator())
                                        .withEndAction {
                                            binding.ivNewLocationDiscovered.visibility = View.GONE
                                            binding.ivNewLocationDiscovered.rotationY =
                                                90f // reset para próxima vez
                                        }
                                        .start()

                                    binding.tvNewLocationDiscovered.animate()
                                        .alpha(0f)
                                        .setDuration(500)
                                        .withEndAction {
                                            binding.tvNewLocationDiscovered.visibility = View.GONE
                                        }
                                        .start()
                                }
                            }
                            .start()
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
                        Log.d("DestinationMapFragment", "Location updated: $location")
                        location?.let {
                            val currentLatLng = LatLng(
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


    private fun createCircularBitmapFromBitmap(
        bitmap: Bitmap,
        targetSize: Int = 200, // Tamaño fijo para todas las imágenes
        borderColor: Int = Color.rgb(144, 74, 69), // #904A45
        borderWidth: Float = 4f
    ): Bitmap {
        val output = createBitmap(targetSize, targetSize)
        val canvas = Canvas(output)

        // Crear pintura para el borde
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        borderPaint.color = borderColor
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = borderWidth

        // Crear pintura para la imagen
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        // Redimensionar la imagen para ajustarse al tamaño fijo
        val matrix = Matrix()
        val scale = targetSize / bitmap.width.toFloat()
        matrix.setScale(scale, scale)

        // Centrar la imagen si no es cuadrada
        if (bitmap.width != bitmap.height) {
            matrix.postTranslate(
                (targetSize - bitmap.width * scale) / 2f,
                (targetSize - bitmap.height * scale) / 2f
            )
        }

        shader.setLocalMatrix(matrix)
        imagePaint.shader = shader

        val radius = (targetSize / 2f) - (borderWidth / 2f)
        // Dibujar la imagen circular
        canvas.drawCircle(targetSize / 2f, targetSize / 2f, radius, imagePaint)
        // Dibujar el borde
        canvas.drawCircle(targetSize / 2f, targetSize / 2f, radius, borderPaint)

        return output
    }

    suspend fun loadBitmapFromUrl(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpsURLConnection
            connection.doInput = true
            connection.connect()
            val inputStream = connection.inputStream
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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
