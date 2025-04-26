package dadm.grupo.dadmproyecto.ui.destinationmap

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
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
import dadm.grupo.dadmproyecto.ui.geofence.GeofenceBroadcastReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import javax.inject.Inject

@HiltViewModel
class DestinationMapViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val locationRepository: LocationsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val upvPosition = LatLng(UPV_POSITION_LATITUDE, UPV_POSITION_LONGITUDE)

    private val _visitedLocations =
        MutableStateFlow(emptyList<dadm.grupo.dadmproyecto.domain.model.Location>())
    val visitedLocations: StateFlow<List<dadm.grupo.dadmproyecto.domain.model.Location>> =
        _visitedLocations.asStateFlow()

    private val _allLoctions =
        MutableStateFlow(emptyList<dadm.grupo.dadmproyecto.domain.model.Location>())

    private val _notVisitedLocations =
        MutableStateFlow(emptyList<dadm.grupo.dadmproyecto.domain.model.Location>())

    private val _lastDiscoveredLocationId =
        MutableStateFlow<Long?>(null)

    private val _lastDiscoveredLocation =
        MutableStateFlow<dadm.grupo.dadmproyecto.domain.model.Location?>(null)

    val lastDiscoveredLocation: StateFlow<dadm.grupo.dadmproyecto.domain.model.Location?> =
        _lastDiscoveredLocation.asStateFlow()

    private val _mapStyle = MutableStateFlow(loadJsonFromFile(context, STANDARD_MAP_STYLE))
    val mapStyle: StateFlow<String> = _mapStyle.asStateFlow()


    private val _isFabMenuOpen = MutableStateFlow(false)
    val isFabMenuOpenState: StateFlow<Boolean> = _isFabMenuOpen.asStateFlow()

    private val _isMapCenteredInUserLocation =
        MutableStateFlow(false)

    val isMapCenteredInUserLocation: StateFlow<Boolean> = _isMapCenteredInUserLocation.asStateFlow()


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

    var geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)
    private val geofenceList = mutableListOf<Geofence>()
    private lateinit var geofencePendingIntent: PendingIntent


    init {
        viewModelScope.launch {
            Log.d("DestinationMapViewModel", "Fetching locations from repository")
            _allLoctions.value = locationRepository.getLocations()
            Log.d("DestinationMapViewModel", "Locations fetched: ${_allLoctions.value}")

            val userId = authRepository.getCurrentUser()?.id

            Log.d("DestinationMapViewModel", "User ID: $userId")

            if (userId != null) {
                _visitedLocations.value = locationRepository.getMyLocationsVisited(userId)
                Log.d(
                    "DestinationMapViewModel",
                    "Visited locations fetched: ${_visitedLocations.value}"
                )


                val visitedLocationIds = _visitedLocations.value.map { it.id }

                _notVisitedLocations.value = _allLoctions.value.filter { location ->
                    !visitedLocationIds.contains(location.id)
                }

                Log.d(
                    "DestinationMapViewModel",
                    "Not visited locations: ${_notVisitedLocations.value}"
                )
            } else {
                Log.d("DestinationMapViewModel", "User ID is null")
            }

            createGeofencesFromLocations(_notVisitedLocations.value)
            Log.d("DestinationMapViewModel", "Geofences created for not visited locations")

        }

        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            currentUser?.id?.let { userId ->
                val myLocationsVisited = locationRepository.getMyLocationsVisited(userId)
                Log.d("DestinationMapViewModel", "My locations visited: $myLocationsVisited")
            }
        }

        viewModelScope.launch {
            GeofenceEventChannel.geofenceEvents.collect { locationId ->
                Log.d(
                    "DestinationMapViewModel",
                    "Geofence event triggered for location ID: $locationId"
                )
                _lastDiscoveredLocationId.value = locationId
                val location = _allLoctions.value.find { it.id == locationId }

                _lastDiscoveredLocation.value = location

                if (!_visitedLocations.value.any { it.id == locationId }) {
                    _visitedLocations.value += location ?: return@collect
                }

                _notVisitedLocations.value =
                    _notVisitedLocations.value.filter { it.id != locationId }
            }
        }
    }


    fun resetLastDiscoveredLocation() {
        _lastDiscoveredLocation.value = null
    }

    fun toggleFabMenu() {
        _isFabMenuOpen.value = !_isFabMenuOpen.value
    }

    fun changeMapCenterInUserLocation() {
        _isMapCenteredInUserLocation.value = !_isMapCenteredInUserLocation.value
        Log.d(
            "CenterLocation",
            "Map centered in user location: ${_isMapCenteredInUserLocation.value}"
        )
    }

    fun setMapCenterInUserLocation(b: Boolean) {
        _isMapCenteredInUserLocation.value = b
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

    private var _lastKnownLocation: MutableStateFlow<Location?> =
        MutableStateFlow(null)
    var lastKnownLocation: StateFlow<Location?> = _lastKnownLocation.asStateFlow()

    fun getAccurateLocationUpdates(): Flow<Location?> =
        callbackFlow {
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation
                    _lastKnownLocation.value = location
                    trySend(location)
                }
            }

            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    locationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )
                } catch (e: SecurityException) {
                    Log.e("DestinationMapViewModel", "Security exception: ${e.message}")
                    close(e)
                }
            } else {
                Log.d("DestinationMapViewModel", "Location permission not granted")
                close(SecurityException("Location permission not granted"))
            }

            awaitClose {
                locationClient.removeLocationUpdates(locationCallback)
            }
        }.flowOn(Dispatchers.IO)


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
                Log.e("GeofenceManager", "Failed to add geofences: ${e}")
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
