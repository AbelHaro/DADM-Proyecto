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
import org.maplibre.android.geometry.LatLng
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

    private val _markers = MutableStateFlow<List<LatLng>>(emptyList())
    val markers: StateFlow<List<LatLng>> = _markers.asStateFlow()

    init {
        loadUserData()
        loadInitialMapData()
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

    private fun loadUserData() {
        viewModelScope.launch {
            _userData.value = firebaseAuth.currentUser
            if (_userData.value == null) {
                _errorMessage.value = "No hay usuario registrado"
            }
        }
    }

    private fun loadInitialMapData() {
        _markers.value = listOf(
            LatLng(40.4168, -3.7038), // Madrid
            LatLng(41.3851, 2.1734)   // Barcelona
        )
    }

    fun addMarker(position: LatLng) {
        _markers.value = _markers.value + position
    }

    fun removeMarker(position: LatLng) {
        _markers.value = _markers.value.filter { it != position }
    }

    fun getUserEmail(): String = _userData.value?.email ?: "No hay usuario registrado"

    fun getUserDisplayName(): String = _userData.value?.displayName ?: "No hay usuario registrado"

    fun getUserId(): String = _userData.value?.uid ?: "No hay usuario registrado"

    sealed class NavigationEvent {
        object NavigateToAuth : NavigationEvent()
    }
}
