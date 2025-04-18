package dadm.grupo.dadmproyecto.ui.destinationmap

import android.app.Application
import android.location.Location
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import dadm.grupo.dadmproyecto.model.POI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import javax.inject.Inject

@HiltViewModel
class DestinationMapViewModel @Inject constructor(
    application: Application,
    private val firebaseAuth: FirebaseAuth
) : AndroidViewModel(application) {

    private val _userData = MutableStateFlow<FirebaseUser?>(null)
    val userData: StateFlow<FirebaseUser?> = _userData.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    private val _markers = MutableStateFlow<List<LatLng>>(emptyList())
    val markers: StateFlow<List<LatLng>> = _markers.asStateFlow()

    private val context = application.applicationContext
    private val locationClient = LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 1000)
        .setMinUpdateDistanceMeters(5f)
        .build()

    //POIs (Point of Interest) de prueba
    private val _pois = MutableStateFlow<List<POI>>(listOf(
        POI("1", "LaVella", LatLng(39.482287, -0.348746)),
        POI("2", "ETSInf", LatLng(39.482714, -0.346711))
    ))
    val pois: StateFlow<List<POI>> = _pois.asStateFlow()

    init {
        loadUserData()
        loadInitialMapData()
    }

    fun logout() {
        viewModelScope.launch {
            try {
                firebaseAuth.signOut()
                _navigationEvent.emit(NavigationEvent.NavigateToAuth)
            } catch (e: Exception) {
                _errorMessage.value = "Error al cerrar sesión: ${e.message}"
            }
        }
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _userData.value = firebaseAuth.currentUser
            if (_userData.value == null) {
                _errorMessage.value = "No hay usuario registrado"
            }
        }
    }

    private fun loadInitialMapData() {
        _markers.value = listOf(
            LatLng(40.4168, -3.7038), // Madrid
            LatLng(41.3851, 2.1734)   // Barcelona
        )
    }

    fun addMarker(position: LatLng) {
        _markers.value = _markers.value + position
    }

    fun removeMarker(position: LatLng) {
        _markers.value = _markers.value.filter { it != position }
    }

    fun getUserEmail(): String = _userData.value?.email ?: "No hay usuario registrado"

    fun getUserDisplayName(): String = _userData.value?.displayName ?: "No hay usuario registrado"

    fun getUserId(): String = _userData.value?.uid ?: "No hay usuario registrado"

    sealed class NavigationEvent {
        object NavigateToAuth : NavigationEvent()
    }

    // Mantiene actualizada la ubicación del usuario
    fun getAccurateLocationUpdates(): Flow<Location?> = callbackFlow @androidx.annotation.RequiresPermission(
        allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION]
    ) {
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                trySend(locationResult.lastLocation)
            }
        }

        locationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        awaitClose {
            locationClient.removeLocationUpdates(locationCallback)
        }
    }.flowOn(Dispatchers.IO)

    // Comprueba si el usuario pasa por algun POI
    fun isNearLocation(user: LatLng, poi: LatLng, radiusMeters: Double = 50.0): Boolean {
        val results = FloatArray(1)
        Location.distanceBetween(
            user.latitude, user.longitude,
            poi.latitude, poi.longitude,
            results
        )
        return results[0] <= radiusMeters
    }

    fun updatePOIs(newPOIs: List<POI>) {
        _pois.value = newPOIs
    }

}
