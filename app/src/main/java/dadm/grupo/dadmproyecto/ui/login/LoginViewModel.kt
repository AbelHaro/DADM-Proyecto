package dadm.grupo.dadmproyecto.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    fun loginUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            onResult(false, context.getString(R.string.login_error_empty_fields))
            return
        }

        viewModelScope.launch {
            try {
                authRepository.signInWithEmail(email, password)
                    .onSuccess {
                        onResult(true, null)
                    }
                    .onFailure { exception ->
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


    /*
    Método para iniciar sesión sin verificar el email.
    Sólo para test y desarrollo.
     */
//    fun loginUserTestNoEmailVerifed(
//        email: String,
//        password: String,
//        onResult: (Boolean, String?) -> Unit
//    ) {
//        if (email.isBlank() || password.isBlank()) {
//            onResult(false, context.getString(R.string.login_error_empty_fields))
//            return
//        }
//
//        viewModelScope.launch {
//            try {
//                f.signInWithEmailAndPassword(email, password)
//                    .addOnCompleteListener { task ->
//                        when {
//                            task.isSuccessful -> onResult(true, null)
//                            else -> onResult(
//                                false,
//                                task.exception?.message
//                                    ?: context.getString(R.string.login_error_unknown)
//                            )
//                        }
//                    }
//            } catch (e: Exception) {
//                onResult(false, e.message ?: context.getString(R.string.login_error_unknown))
//            }
//        }
//    }

}
