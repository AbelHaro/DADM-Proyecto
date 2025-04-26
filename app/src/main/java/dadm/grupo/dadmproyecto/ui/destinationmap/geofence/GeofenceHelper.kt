package dadm.grupo.dadmproyecto.ui.destinationmap.geofence

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import dadm.grupo.dadmproyecto.R

object GeofenceHelper {
    private const val CHANNEL_ID = "geofence_channel"

    fun showLocationNotification(context: Context, locationId: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        // Build the notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("You've arrived!")
            .setContentText("You've reached location with ID: $locationId")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Show the notification
        notificationManager.notify(locationId.hashCode(), notification)
    }
}
