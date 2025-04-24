package dadm.grupo.dadmproyecto.ui.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import dadm.grupo.dadmproyecto.data.auth.AuthRepository
import dadm.grupo.dadmproyecto.data.db.LocationsVisitedRepository
import dadm.grupo.dadmproyecto.ui.destinationmap.GeofenceEventChannel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var locationsVisitedRepository: LocationsVisitedRepository

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e("GeofenceBroadcastReceiver", "Geofence error: $errorMessage")
            return
        }

        // Get the transition type
        val geofenceTransition = geofencingEvent.geofenceTransition

        // Check if the transition is an entrance
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // Get the geofences that were triggered
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            // Handle the geofences
            triggeringGeofences?.forEach { geofence ->
                // Get the location ID from the geofence request ID
                val locationId = geofence.requestId

                Log.d("GeofenceBroadcastReceiver", "Entered location: $locationId")
                CoroutineScope(Dispatchers.IO).launch {
                    val userInfo = authRepository.getCurrentUser()

                    if (userInfo == null) {
                        Log.e("GeofenceBroadcastReceiver", "User not logged in")
                        return@launch
                    }
                    locationsVisitedRepository.insertLocationVisited(
                        userInfo.id,
                        locationId.toLong()
                    )

                    GeofenceEventChannel.triggerGeofenceEvent(locationId.toLong())
                }

                GeofenceHelper.showLocationNotification(context, locationId)
            }
        }
    }
}
