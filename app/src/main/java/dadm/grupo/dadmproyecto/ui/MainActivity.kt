package dadm.grupo.dadmproyecto.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.data.auth.AuthRepository
import dadm.grupo.dadmproyecto.databinding.ActivityMainBinding
import dadm.grupo.dadmproyecto.utils.PermissionUtils
import dadm.grupo.dadmproyecto.utils.PermissionUtils.requestBackgroundLocationPermission
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                showPermissionDeniedDialog()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = binding.fcvActivityMain.getFragment<NavHostFragment>().navController
        binding.bnvActivityMain.setupWithNavController(navController)

        // Apply padding for system bars specifically to the BottomNavigationView
        ViewCompat.setOnApplyWindowInsetsListener(binding.bnvActivityMain) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply padding for left, right, and bottom insets
            view.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            // Return the insets for potential consumption by other views if needed
            insets
        }

        try {
            if (!PermissionUtils.hasLocationPermission(this@MainActivity)) {
                Log.d("MainActivity", "Requesting location permission")
                PermissionUtils.requestLocationPermission(this@MainActivity)
            }

            if (!PermissionUtils.hasBackgroundLocationPermission(this@MainActivity)) {
                Log.d("MainActivity", "Requesting background location permission")
                requestBackgroundLocationPermission(this@MainActivity)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking permissions or login: ${e.message}")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (isOnInitialDestination()) {
            showExitConfirmationDialog()
        } else {
            super.onBackPressed()
        }
    }

    private fun isOnInitialDestination(): Boolean {
        return navController.currentDestination?.id == navController.graph.startDestinationId
    }


    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.exit_dialog_title)
            .setMessage(R.string.exit_dialog_message)
            .setPositiveButton(R.string.exit_dialog_positive) { _, _ -> finishAffinity() }
            .setNegativeButton(R.string.exit_dialog_negative) { dialog, _ -> dialog.dismiss() }
            .setCancelable(true)
            .create()
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.location_permission_title)
            .setMessage(R.string.location_permission_message)
            .setPositiveButton(R.string.go_to_settings) { _, _ -> openAppSettings() }
            .setCancelable(false)
            .create()
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}
