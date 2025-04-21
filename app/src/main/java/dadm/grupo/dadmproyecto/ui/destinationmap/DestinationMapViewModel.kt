package dadm.grupo.dadmproyecto.ui.destinationmap

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dadm.grupo.dadmproyecto.data.auth.AuthRepository
import dadm.grupo.dadmproyecto.data.db.LocationsRepository
import dadm.grupo.dadmproyecto.model.POI
import dadm.grupo.dadmproyecto.ui.geofence.GeofenceBroadcastReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val application: Application,
    private val authRepository: AuthRepository,
    private val locationRepository: LocationsRepository,
    private val locationManager: LocationManager,
    @ApplicationContext private val context: Context
) : AndroidViewModel(application) {

    val upvPosition = LatLng(UPV_POSITION_LATITUDE, UPV_POSITION_LONGITUDE)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    private val _markers =
        MutableStateFlow(emptyList<dadm.grupo.dadmproyecto.domain.model.Location>())
    val markers: StateFlow<List<dadm.grupo.dadmproyecto.domain.model.Location>> =
        _markers.asStateFlow()

    private val _mapStyle = MutableStateFlow(loadJsonFromFile(context, STANDARD_MAP_STYLE))
    val mapStyle: StateFlow<String> = _mapStyle.asStateFlow()

    private val _myPosition = MutableStateFlow(getLastKnownLocation())
    val myPosition: StateFlow<Location?> = _myPosition.asStateFlow()

    private val isFabMenuOpen = MutableStateFlow(false)
    val isFabMenuOpenState: StateFlow<Boolean> = isFabMenuOpen.asStateFlow()

    private var _isLocationPermissionGranted =
        MutableStateFlow(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
        )
    val isLocationPermissionGranted: StateFlow<Boolean> = _isLocationPermissionGranted.asStateFlow()

    private val locationClient = LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(1000)
        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        .setMinUpdateDistanceMeters(5f)
        .build()

    var geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    private val geofenceList = mutableListOf<Geofence>()
    private lateinit var geofencePendingIntent: PendingIntent

    private val _pois = MutableStateFlow<List<POI>>(
        listOf(
            POI("1", "LaVella", LatLng(39.482287, -0.348746)),
            POI("2", "ETSInf", LatLng(39.482714, -0.346711))
        )
    )
    val pois: StateFlow<List<POI>> = _pois.asStateFlow()

    init {
        viewModelScope.launch {
            Log.d("DestinationMapViewModel", "Fetching locations from repository")
            _markers.value = locationRepository.getLocations()
            Log.d("DestinationMapViewModel", "Locations fetched: ${_markers.value}")
            createGeofencesFromLocations(_markers.value)
            Log.d("DestinationMapViewModel", "Geofences created: $geofenceList")
        }

        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            currentUser?.id?.let { userId ->
                val myLocationsVisited = locationRepository.getMyLocationsVisited(userId)
                Log.d("DestinationMapViewModel", "My locations visited: $myLocationsVisited")
            }
        }
    }

    fun toggleFabMenu() {
        isFabMenuOpen.value = !isFabMenuOpen.value
    }

    fun navigateToAuth() {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.NavigateToAuth)
        }
    }

    private fun getLastKnownLocation(): Location? {
        return if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } else {
            null
        }
    }

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
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
    }

    fun updateCurrentLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            _myPosition.value = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }
    }

    sealed class NavigationEvent {
        object NavigateToAuth : NavigationEvent()
    }

    fun getAccurateLocationUpdates(): Flow<Location?> =
        callbackFlow @androidx.annotation.RequiresPermission(
            allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION]
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

    fun isNearLocation(user: LatLng, poi: LatLng, radiusMeters: Double = 50.0): Boolean {
        val results = FloatArray(1)
        Location.distanceBetween(
            user.latitude,
            user.longitude,
            poi.latitude,
            poi.longitude,
            results
        )
        return results[0] <= radiusMeters
    }

    fun updatePOIs(newPOIs: List<POI>) {
        _pois.value = newPOIs
    }

    private fun createGeofencesFromLocations(locations: List<dadm.grupo.dadmproyecto.domain.model.Location>) {
        geofenceList.clear()
        locations.forEach { location ->
            geofenceList.add(
                Geofence.Builder()
                    .setRequestId(location.id.toString())
                    .setCircularRegion(
                        location.latitude,
                        location.longitude,
                        location.radius.toFloat()
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build()
            )
        }
        if (geofenceList.isNotEmpty()) {
            addGeofences()
        }
    }

    @SuppressLint("MissingPermission")
    fun addGeofences() {
        if (!isLocationPermissionGranted.value) {
            Log.d("GeofenceManager", "Location permission not granted")
            return
        }

        if (geofenceList.isEmpty()) {
            Log.d("GeofenceManager", "No geofences to add")
            return
        }

        val geofencingRequest = GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()

        geofencePendingIntent = getPendingIntent()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
            .addOnSuccessListener {
                Log.d("GeofenceManager", "Geofences added successfully")
            }
            .addOnFailureListener { e ->
                Log.e("GeofenceManager", "Failed to add geofences: ${e.message}")
            }
    }

    private fun getPendingIntent(): PendingIntent {
        if (::geofencePendingIntent.isInitialized) return geofencePendingIntent

        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        geofencePendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        return geofencePendingIntent
    }
}
