package dadm.grupo.dadmproyecto.ui.register

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    @Inject
    lateinit var supabaseClient: SupabaseClient

    fun registerUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        when {
            email.isBlank() || password.isBlank() -> {
                onResult(false, context.getString(R.string.register_error_empty_fields))
                return
            }

            !isValidEmail(email) -> {
                onResult(false, context.getString(R.string.register_error_invalid_email))
                return
            }

            !isValidPassword(password) -> {
                onResult(false, context.getString(R.string.register_error_weak_password))
                return
            }
        }

        viewModelScope.launch {
            try {
                val result = authRepository.signUpWithEmail(email, password)

                Log.d("RegisterViewModel", "Result: $result")

                if (result.isSuccess) {

                    onResult(true, context.getString(R.string.register_success))
                } else {
                    onResult(false, context.getString(R.string.register_error_unknown))
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: context.getString(R.string.register_error_unknown))
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPassword(password: String): Boolean {
        return when {
            password.length !in 8..255 -> false
            password.any { it.isWhitespace() } -> false
            !password.any { it.isDigit() } -> false
            !password.any { it.isLetter() } -> false
            !password.any { it.isUpperCase() } -> false
            !password.any { it.isLowerCase() } -> false
            else -> true
        }
    }

    private fun handleRegistrationResult(
        task: Task<AuthResult>,
        onResult: (Boolean, String?) -> Unit
    ) {
        when {
            task.isSuccessful -> {
                firebaseAuth.currentUser?.let { user ->
                    user.sendEmailVerification()
                        .addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                onResult(true, context.getString(R.string.register_success))
                            } else {
                                onResult(
                                    false, verificationTask.exception?.message
                                        ?: context.getString(R.string.register_error_unknown)
                                )
                            }
                        }
                } ?: onResult(false, context.getString(R.string.register_error_unknown))
            }

            else -> {
                onResult(
                    false, task.exception?.message
                        ?: context.getString(R.string.register_error_unknown)
                )
            }
        }
    }
}
