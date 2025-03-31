package dadm.grupo.dadmproyecto.ui.destinationmap

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext val context: Context
) : ViewModel() {

    val UPV_POSITION = LatLng(39.4818, -0.3432)

    private val _userData = MutableStateFlow(firebaseAuth.currentUser)
    val userData: StateFlow<FirebaseUser?> = _userData.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    private val _markers = MutableStateFlow(
        listOf(
            LatLng(39.482591772922085, -0.3462456083400781)
        )
    )
    val markers: StateFlow<List<LatLng>> = _markers.asStateFlow()

    private val _mapStyle =
        MutableStateFlow(loadJsonFromFile(context, "standardMapStyle.json"))
    val mapStyle: StateFlow<String> = _mapStyle.asStateFlow()


    fun logout() = viewModelScope.launch {
        try {
            firebaseAuth.signOut()
            _navigationEvent.emit(NavigationEvent.NavigateToAuth)
        } catch (e: Exception) {
            _errorMessage.value = "Error al cerrar sesión: ${e.message}"
        }
    }

    fun addMarker(position: LatLng) {
        _markers.value += position
    }

    fun removeMarker(position: LatLng) {
        _markers.value = _markers.value.filter { it != position }
    }

    fun getUserEmail(): String = _userData.value?.email ?: "No hay usuario registrado"
    fun getUserDisplayName(): String = _userData.value?.displayName ?: "No hay usuario registrado"
    fun getUserId(): String = _userData.value?.uid ?: "No hay usuario registrado"

    sealed class NavigationEvent {
        data object NavigateToAuth : NavigationEvent()
    }

    /**
     * Loads the map style JSON from the assets folder.
     * The file is located at assets/satelliteMapStyle.json
     */
    private fun loadJsonFromFile(context: Context, filePath: String): String {
        return try {
            context.assets.open(filePath).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "{}"
        }
    }
}
