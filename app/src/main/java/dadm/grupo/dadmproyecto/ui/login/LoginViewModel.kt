package dadm.grupo.dadmproyecto.ui.login

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.data.auth.AuthRepository
import dadm.grupo.dadmproyecto.data.db.UsersRepository
import dadm.grupo.dadmproyecto.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val usersRepository: UsersRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    @Inject
    lateinit var supabaseClient: SupabaseClient

    fun loginUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            onResult(false, context.getString(R.string.login_error_empty_fields))
            return
        }

        viewModelScope.launch {
            try {
                authRepository.signInWithEmail(email, password)
                    .onSuccess {
                        val user = authRepository.getCurrentUser()

                        if (user != null) {
                            Log.d("LoginViewModel", "User: $user")
                            val userId = user.id

                            val userExists = usersRepository.getMyUserData()


                            // Si el usuario ya existe, no lo creamos de nuevo, solo se crea la primera vez que inicia sesión
                            if (userExists != null) {
                                Log.d("LoginViewModel", "User already exists: $userExists")
                                onResult(true, null)
                                return@onSuccess
                            }


                            val userData = User(
                                userId = userId,
                                displayName = email.substringBefore("@"),
                                bio = "Esta es tu biografía.\nPuedes editarla en la sección de configuración.",
                            )

                            Log.d("LoginViewModel", "Inserting this User data: $userData")


                            supabaseClient.from("users").insert(userData)

                            Log.d("LoginViewModel", "User data inserted successfully")
                        } else {
                            Log.d("LoginViewModel", "User is null after registration")
                        }
                        onResult(true, null)
                    }
                    .onFailure { exception ->
                        Log.d("LoginViewModel", "Login failed: ${exception.message}")
                        onResult(
                            false,
                            exception.message ?: context.getString(R.string.login_error_unknown)
                        )
                    }
            } catch (e: Exception) {
                onResult(false, e.message ?: context.getString(R.string.login_error_unknown))
            }
        }
    }


}
