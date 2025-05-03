package dadm.grupo.dadmproyecto.ui.forgotpassword

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dadm.grupo.dadmproyecto.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _resetStatus = MutableLiveData<Boolean?>()
    val resetStatus: LiveData<Boolean?> = _resetStatus

    fun validateEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun sendPasswordResetEmail(email: String) {

        if (!validateEmail(email)) {
            _resetStatus.postValue(false)
            return
        }

        viewModelScope.launch {
            try {
                Log.d("ForgotPassword", "Sending password reset email to $email")
                authRepository.forgotPassword(email)
                _resetStatus.postValue(true)
            } catch (e: Exception) {
                Log.e("ForgotPassword", "Error sending password reset email", e)
                _resetStatus.postValue(false)
            }
        }
    }
}
