package dadm.grupo.dadmproyecto.ui.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {
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

                // You could send this to a service to process in the background
                // or display a notification to the user
                Log.d("GeofenceBroadcastReceiver", "Entered location: $locationId")

                // Show notification
                GeofenceHelper.showLocationNotification(context, locationId)
            }
        }
    }
}
