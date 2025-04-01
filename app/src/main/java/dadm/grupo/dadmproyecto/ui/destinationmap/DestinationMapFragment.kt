package dadm.grupo.dadmproyecto.ui.destinationmap

import android.annotation.SuppressLint
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

    // Inicialización del fragmento
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inicializa MapLibre con la configuración predeterminada
        MapLibre.getInstance(requireContext(), null, WellKnownTileServer.MapLibre)
    }

    // Se infla la vista del fragmento
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDestinationMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Se configura el mapa una vez que la vista ha sido creada
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this) // Llama a onMapReady cuando el mapa está listo
        observeNavigationEvents() // Observa eventos de navegación
    }

    // Callback cuando el mapa está listo
    @SuppressLint("MissingPermission")
    override fun onMapReady(mapLibreMap: MapLibreMap) {
        this.mapLibreMap = mapLibreMap
        mapLibreMap.setStyle(
            Style.Builder().fromJson(viewModel.mapStyle.value) // Establece el estilo del mapa
        ) { style ->

            // Configura el LocationComponent para rastrear la ubicación
            val locationComponent = mapLibreMap.locationComponent
            val locationComponentOptions = LocationComponentOptions.builder(requireContext())
                .pulseEnabled(true) // Habilita el pulso (efecto visual)
                .build()

            // Crea las opciones para activar el LocationComponent
            val locationComponentActivationOptions =
                buildLocationComponentActivationOptions(style, locationComponentOptions)

            // Activa el LocationComponent y habilita el seguimiento de la ubicación
            locationComponent.activateLocationComponent(locationComponentActivationOptions)
            locationComponent.isLocationComponentEnabled = true
            locationComponent.cameraMode = CameraMode.TRACKING

            // Añade los marcadores al mapa
            viewModel.markers.value.forEach { latLng ->
                val markerOptions = MarkerOptions()
                    .snippet("Marker")
                    .position(latLng)
                    .title("Marker")
                Log.d("DestinationMapFragment", "Adding marker at: $latLng")
                mapLibreMap.addMarker(markerOptions) // Agrega el marcador
            }
        }

        // Configura la posición de la cámara en el mapa
        mapLibreMap.cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
            .target(
                org.maplibre.android.geometry.LatLng(
                    viewModel.UPV_POSITION.latitude,
                    viewModel.UPV_POSITION.longitude
                )
            )
            .zoom(15.0) // Establece el nivel de zoom
            .bearing(20.7) // Dirección de la cámara
            .tilt(20.0) // Inclinación de la cámara
            .build()

        // Establece los límites de zoom y el área visible
        mapLibreMap.setMinZoomPreference(15.0)
        mapLibreMap.setMaxZoomPreference(20.0)
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
                viewModel.navigationEvent.collect { event ->
                    when (event) {
                        is DestinationMapViewModel.NavigationEvent.NavigateToAuth -> navigateToAuth() // Maneja la navegación
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isFabMenuOpenState.collect { isMenuOpen ->
                    // Cambia la visibilidad de los botones de estilo dependiendo de isFabMenuOpen
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
                    ) // Cambia el estilo del mapa
                }
            }
        }


        // Manejo de los clics en los FABs
        binding.fabMain.setOnClickListener {
            // Al presionar el FAB principal, cambia el estado de visibilidad del menú
            viewModel.toggleFabMenu()
        }

        binding.fabStyleStandard.setOnClickListener {
            // Cambia el estilo del mapa a estándar
            Log.d("DestinationMapFragment", "Standard style selected")
            viewModel.changeMapStyle(viewModel.STANDARD_MAP_STYLE) // Cambia el estilo del mapa
            viewModel.toggleFabMenu()
        }

        binding.fabStyleSatellite.setOnClickListener {
            // Cambia el estilo del mapa a satélite
            Log.d("DestinationMapFragment", "Satellite style selected")
            viewModel.changeMapStyle(viewModel.SATELLITE_MAP_STYLE) // Cambia el estilo del mapa
            viewModel.toggleFabMenu() // Cierra el menú después de seleccionar el estilo
        }
    }

    // Función para navegar a la actividad de autenticación
    private fun navigateToAuth() {
        Intent(requireActivity(), AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }.also { intent ->
            startActivity(intent)
            requireActivity().finish() // Cierra la actividad actual
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
        binding.mapView.onDestroy() // Limpia los recursos del MapView
        _binding = null
        mapLibreMap = null // Elimina la referencia al mapa
    }
}
