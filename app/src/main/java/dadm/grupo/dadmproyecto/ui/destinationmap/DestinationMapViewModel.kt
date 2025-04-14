package dadm.grupo.dadmproyecto.ui.destinationmap

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dadm.grupo.dadmproyecto.data.auth.AuthRepository
import dadm.grupo.dadmproyecto.data.db.LocationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import javax.inject.Inject

@HiltViewModel
class DestinationMapViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val locationRepository: LocationsRepository,
    private val locationManager: LocationManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Coordenadas de la UPV
    val upvPosition = LatLng(UPV_POSITION_LATITUDE, UPV_POSITION_LONGITUDE)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    private val _markers =
        MutableStateFlow(emptyList<dadm.grupo.dadmproyecto.domain.model.Location>())
    val markers: StateFlow<List<dadm.grupo.dadmproyecto.domain.model.Location>> =
        _markers.asStateFlow()

    private val _mapStyle =
        MutableStateFlow(loadJsonFromFile(context, STANDARD_MAP_STYLE))
    val mapStyle: StateFlow<String> = _mapStyle.asStateFlow()

    private val _myPosition =
        MutableStateFlow(getLastKnownLocation())
    val myPosition: StateFlow<Location?> = _myPosition.asStateFlow()

    private val isFabMenuOpen = MutableStateFlow(false)
    val isFabMenuOpenState: StateFlow<Boolean> = isFabMenuOpen.asStateFlow()

    private var _isLocationPermissionGranted =
        MutableStateFlow(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    val isLocationPermissionGranted: StateFlow<Boolean> =
        _isLocationPermissionGranted.asStateFlow()


    init {
        Log.d("DestinationMapViewModel", "Initializing ViewModel")
        viewModelScope.launch {
            Log.d("DestinationMapViewModel", "Fetching locations from repository")
            _markers.value = locationRepository.getLocations()
            Log.d("DestinationMapViewModel", "Locations: ${_markers.value}")
        }

    }


    // Toggle para cambiar el estado de visibilidad del menú FAB
    fun toggleFabMenu() {
        isFabMenuOpen.value = !isFabMenuOpen.value
    }

    // Navega a la pantalla de autenticación
    fun navigateToAuth() {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.NavigateToAuth)
        }
    }

    // Recupera la última ubicación conocida
    private fun getLastKnownLocation(): Location? {
        return if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } else {
            null
        }
    }

    // Carga el archivo JSON de un mapa desde los recursos
    private fun loadJsonFromFile(context: Context, fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }

    fun changeMapStyle(mapStyle: String) {
        if (_mapStyle.value != mapStyle) {
            _mapStyle.value = loadJsonFromFile(context, mapStyle)
        }
    }

    fun checkLocationPermission() {
        _isLocationPermissionGranted.value =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun updateCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            _myPosition.value = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }
    }

    // Manejo de eventos de navegación
    sealed class NavigationEvent {
        object NavigateToAuth : NavigationEvent()
    }

}
