package dadm.grupo.dadmproyecto.ui.destinationmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DestinationMapViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _userData = MutableStateFlow<FirebaseUser?>(null)
    val userData: StateFlow<FirebaseUser?> = _userData.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    private val _showConfirmationDialog = MutableStateFlow(false)
    val showConfirmationDialog: StateFlow<Boolean> = _showConfirmationDialog.asStateFlow()

    init {
        loadUserData()
    }

    fun logout() {
        viewModelScope.launch {
            try {
                firebaseAuth.signOut()
                _navigationEvent.emit(NavigationEvent.NavigateToAuth)
            } catch (e: Exception) {
                _errorMessage.value = "Error al cerrar sesión: ${e.message}"
            }
        }
    }

    fun showLogoutConfirmation() {
        _showConfirmationDialog.value = true
    }

    fun dismissLogoutConfirmation() {
        _showConfirmationDialog.value = false
    }

    fun confirmLogout() {
        viewModelScope.launch {
            try {
                firebaseAuth.signOut()
                _navigationEvent.emit(NavigationEvent.NavigateToAuth)
            } catch (e: Exception) {
                _errorMessage.value = "Error al cerrar sesión: ${e.message}"
            } finally {
                _showConfirmationDialog.value = false
            }
        }
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                _userData.value = firebaseAuth.currentUser
                if (_userData.value == null) {
                    _errorMessage.value = "No hay usuario registrado"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar datos del usuario: ${e.message}"
            }
        }
    }

    fun getUserEmail(): String = userData.value?.email ?: "Usuario no identificado"
    fun getUserDisplayName(): String = userData.value?.displayName ?: "Nombre no disponible"
    fun getUserId(): String = userData.value?.uid ?: "ID no disponible"

    sealed class NavigationEvent {
        object NavigateToAuth : NavigationEvent()
    }
}
