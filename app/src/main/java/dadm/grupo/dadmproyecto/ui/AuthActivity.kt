package dadm.grupo.dadmproyecto.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.data.auth.AuthRepository
import dadm.grupo.dadmproyecto.databinding.ActivityAuthBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    private lateinit var navController: NavController
    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lanzar una corrutina para saber si el usuario está logueado de una sesión anterior
        lifecycleScope.launch {
            try {

                if (authRepository.isUserLoggedIn()) {
                    redirectToMain()
                    return@launch
                }

            } catch (e: Exception) {
                Log.e("AuthActivity", "Error signing in user: ${e.message}")
            }

            // Continuar con la UI si no está logueado
            WindowCompat.setDecorFitsSystemWindows(window, false)
            binding = ActivityAuthBinding.inflate(layoutInflater)
            setContentView(binding.root)

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
        }
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
