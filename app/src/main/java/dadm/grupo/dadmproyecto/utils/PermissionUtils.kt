package dadm.grupo.dadmproyecto.utils

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

object PermissionUtils {

    private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private const val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 1002

    fun hasLocationPermission(activity: AppCompatActivity): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestLocationPermission(activity: AppCompatActivity) {
        if (!hasLocationPermission(activity)) {
            activity.requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    fun requestBackgroundLocationPermission(activity: AppCompatActivity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                AlertDialog.Builder(activity)
                    .setTitle("Permiso de ubicación en segundo plano")
                    .setMessage("Esta aplicación necesita acceder a tu ubicación en segundo plano para que las geocercas funcionen correctamente.")
                    .setPositiveButton("Permitir") { _, _ ->
                        activity.requestPermissions(
                            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                            BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE
                        )
                    }
                    .setNegativeButton("Volver", null)
                    .show()
            }
        }
    }

    fun hasBackgroundLocationPermission(activity: AppCompatActivity): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // En versiones anteriores no hace falta
        }
    }
}
