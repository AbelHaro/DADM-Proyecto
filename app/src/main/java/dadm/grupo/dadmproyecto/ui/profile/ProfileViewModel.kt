package dadm.grupo.dadmproyecto.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dadm.grupo.dadmproyecto.data.auth.AuthRepository
import dadm.grupo.dadmproyecto.data.db.LocationsRepository
import dadm.grupo.dadmproyecto.data.db.UsersRepository
import dadm.grupo.dadmproyecto.domain.model.Location
import dadm.grupo.dadmproyecto.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UsersRepository,
    private val locationsRepository: LocationsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> get() = _userState

    private val _locationsVisitedCount = MutableStateFlow(0)
    val locationsVisitedCount: StateFlow<Int> get() = _locationsVisitedCount

    private val _locationsVisited = MutableStateFlow<List<Location>>(emptyList())
    val locationsVisited: StateFlow<List<Location>> get() = _locationsVisited

    private val _allLocations = MutableStateFlow<List<Location>>(emptyList())
    val allLocations: StateFlow<List<Location>> get() = _allLocations

    private val _logoutState = MutableStateFlow<Result<Boolean>?>(null)
    val logoutState: StateFlow<Result<Boolean>?> get() = _logoutState

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                userRepository.getMyUserData()?.let { user ->
                    _userState.value = user
                    loadLocationsVisited(user.userId)
                    loadAllLocations()
                } ?: run {
                    // Manejo de error, opcional
                    _userState.value = null
                }
            } catch (e: Exception) {
                // Manejo de error, opcional
                e.printStackTrace()
            }
        }
    }

    private suspend fun loadLocationsVisited(userId: String) {
        try {
            val visitedLocations = locationsRepository.getMyLocationsVisited(userId)
            _locationsVisited.value = visitedLocations
            _locationsVisitedCount.value = visitedLocations.size
        } catch (e: Exception) {
            e.printStackTrace()
            _locationsVisitedCount.value = 0
            _locationsVisited.value = emptyList()
        }
    }

    fun loadAllLocations() {
        viewModelScope.launch {
            try {
                val allLocations = locationsRepository.getLocations()
                _allLocations.value = allLocations
            } catch (e: Exception) {
                e.printStackTrace()
                _allLocations.value = emptyList()
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                val result = authRepository.signOut()
                _logoutState.value = result
            } catch (e: Exception) {
                _logoutState.value = Result.failure(e)
            }
        }
    }
}
