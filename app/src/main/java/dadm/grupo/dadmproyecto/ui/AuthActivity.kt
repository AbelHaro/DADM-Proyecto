package dadm.grupo.dadmproyecto.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.auth.FirebaseAuth
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.databinding.ActivityAuthBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavController
    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar sesión antes de mostrar la UI
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            redirectToMain()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = binding.authFragmentContainer.getFragment<NavHostFragment>().navController

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun redirectToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish() // Importante para que no se pueda volver atrás
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (isOnInitialDestination()) {
            showExitConfirmationDialog()
        } else {
            super.onBackPressed() // Let Navigation Component handle back press
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
