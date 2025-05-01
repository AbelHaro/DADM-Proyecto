package dadm.grupo.dadmproyecto.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.data.auth.AuthRepository
import dadm.grupo.dadmproyecto.databinding.ActivityMainBinding
import dadm.grupo.dadmproyecto.ui.main.NetworkViewModel
import dadm.grupo.dadmproyecto.utils.PermissionUtils
import dadm.grupo.dadmproyecto.utils.PermissionUtils.requestBackgroundLocationPermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding
    private val networkViewModel: NetworkViewModel by viewModels()

    private var snackbar: Snackbar? = null

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

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        observeNetworkConnection()

        // Check permissions
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

    private fun observeNetworkConnection() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                Log.d(
                    "MainActivityInternet",
                    "Starting network connection collection within STARTED lifecycle."
                )
                networkViewModel.isConnected.collect { isConnected ->
                    Log.d(
                        "MainActivityInternet",
                        "Collected Network Status in MainActivity: $isConnected"
                    )
                    if (isConnected) {
                        if (snackbar != null) {
                            Log.d("MainActivityInternet", "Network connected. Dismissing Snackbar.")
                            snackbar?.dismiss()
                            snackbar = null
                        } else {
                            Log.d(
                                "MainActivityInternet",
                                "Network connected. No Snackbar to dismiss."
                            )
                        }

                        binding.ivNoInternet.visibility = View.GONE
                        binding.tvNoInternet.visibility = View.GONE

                    } else {
                        if (snackbar == null) {
                            Log.d("MainActivityInternet", "Network disconnected. Showing Snackbar.")
                            showNoInternetSnackbar()
                        } else {
                            Log.d(
                                "MainActivityInternet",
                                "Network disconnected, but Snackbar is already visible."
                            )
                        }

                        binding.ivNoInternet.visibility = View.VISIBLE
                        binding.tvNoInternet.visibility = View.VISIBLE

                    }
                }
                Log.d(
                    "MainActivityInternet",
                    "Stopping network connection collection (lifecycle not STARTED)."
                )
            }
        }
    }

    private fun showNoInternetSnackbar() {
        if (snackbar == null) {
            snackbar = Snackbar.make(
                binding.root,
                R.string.no_internet_connection,
                Snackbar.LENGTH_INDEFINITE
            ).setAction(R.string.retry) {
                // Just reset the snackbar reference.
                snackbar = null
            }
            Log.d("MainActivityInternet", "Showing No Internet Snackbar instance.")
            snackbar?.show()
        } else {
            Log.d(
                "MainActivityInternet",
                "showNoInternetSnackbar called but snackbar already exists."
            )
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
