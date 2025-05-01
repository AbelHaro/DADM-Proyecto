package dadm.grupo.dadmproyecto.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import com.google.android.material.snackbar.Snackbar
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.data.auth.AuthRepository
import dadm.grupo.dadmproyecto.databinding.ActivityAuthBinding
import dadm.grupo.dadmproyecto.ui.main.NetworkViewModel
import dadm.grupo.dadmproyecto.utils.PermissionUtils.hasBackgroundLocationPermission
import dadm.grupo.dadmproyecto.utils.PermissionUtils.hasLocationPermission
import dadm.grupo.dadmproyecto.utils.PermissionUtils.requestBackgroundLocationPermission
import dadm.grupo.dadmproyecto.utils.PermissionUtils.requestLocationPermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    private lateinit var navController: NavController
    private lateinit var binding: ActivityAuthBinding

    private val networkViewModel: NetworkViewModel by viewModels()
    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()

        observeNetworkConnection()

    }

    private fun observeNetworkConnection() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                networkViewModel.isConnected.collect { isConnected ->
                    if (isConnected) {
                        snackbar?.dismiss()  // Si se conecta, oculta el Snackbar
                        snackbar = null  // Elimina el Snackbar cuando la conexión es exitosa
                        lifecycleScope.launch {
                            checkLoginStatus()
                        }
                    } else {

                        showNoInternetSnackbar()  // Si no hay conexión y no hay Snackbar visible, muestra uno nuevo

                    }
                }
            }
        }
    }

    private fun showNoInternetSnackbar() {
        snackbar = Snackbar.make(
            binding.root,
            "No hay conexión a Internet",
            Snackbar.LENGTH_INDEFINITE
        ).setAction("Volver a intentar") {
        }
        snackbar?.show()  // Muestra el Snackbar
    }


    private fun setupUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        navController =
            binding.authFragmentContainer.getFragment<NavHostFragment>().navController

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        if (!hasLocationPermission(this)) {
            requestLocationPermission(this)
        } else if (!hasBackgroundLocationPermission(this)) {
            requestBackgroundLocationPermission(this)
        }
    }

    private suspend fun checkLoginStatus() {
        try {
            if (authRepository.isUserLoggedIn()) {
                redirectToMain()
            }
        } catch (e: Exception) {
            Log.e("AuthActivity", "Error checking login status: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1001 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (!hasBackgroundLocationPermission(this)) {
                        requestBackgroundLocationPermission(this)
                    } else {
                        lifecycleScope.launch {
                            checkLoginStatus()
                        }
                    }
                } else {
                    showPermissionDeniedDialog()
                }
            }

            1002 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    lifecycleScope.launch {
                        checkLoginStatus()
                    }
                } else {
                    showPermissionDeniedDialog()
                }
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permisos de ubicación")
            .setMessage("Esta aplicación necesita acceso a tu ubicación para funcionar correctamente.")
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun redirectToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
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
            .setPositiveButton(R.string.exit_dialog_positive) { _, _ ->
                finishAffinity()
            }
            .setNegativeButton(R.string.exit_dialog_negative) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .create()
            .show()
    }
}
